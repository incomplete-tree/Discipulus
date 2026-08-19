#include "my_application.h"

#include <string.h>

#include <flutter_linux/flutter_linux.h>
#ifdef GDK_WINDOWING_X11
#include <gdk/gdkx.h>
#endif

#include "flutter/generated_plugin_registrant.h"

struct _MyApplication {
  GtkApplication parent_instance;
  char** dart_entrypoint_arguments;
  GtkWindow* window;
  FlMethodChannel* desktop_channel;
  gchar* pending_route;
};

G_DEFINE_TYPE(MyApplication, my_application, GTK_TYPE_APPLICATION)

static void send_desktop_route(MyApplication* self, const gchar* route) {
  if (self->desktop_channel == nullptr || route == nullptr) {
    g_free(self->pending_route);
    self->pending_route = g_strdup(route);
    return;
  }

  g_autoptr(FlValue) arguments = fl_value_new_string(route);
  fl_method_channel_invoke_method(
      self->desktop_channel, "openRoute", arguments, nullptr, nullptr,
      nullptr);
}

static gboolean is_supported_route(const gchar* route) {
  return g_strcmp0(route, "calendar") == 0 ||
         g_strcmp0(route, "grades") == 0 ||
         g_strcmp0(route, "messages") == 0;
}

static gchar* route_from_value(const gchar* value) {
  g_autofree gchar* route = g_ascii_strdown(value == nullptr ? "" : value, -1);
  if (!is_supported_route(route)) {
    return nullptr;
  }
  return g_steal_pointer(&route);
}

static gchar* route_from_argument(const gchar* argument) {
  if (argument == nullptr) {
    return nullptr;
  }

  const gchar* route = nullptr;
  if (g_str_has_prefix(argument, "--route=")) {
    route = argument + strlen("--route=");
  } else if (g_str_has_prefix(argument, "discipulus://")) {
    route = argument + strlen("discipulus://");
  } else {
    return nullptr;
  }

  const gchar* end = strpbrk(route, "/?#");
  g_autofree gchar* candidate = end == nullptr
                                    ? g_strdup(route)
                                    : g_strndup(route, end - route);
  return route_from_value(candidate);
}

static gchar* route_from_arguments(gchar** arguments) {
  for (gchar** argument = arguments; argument != nullptr && *argument != nullptr;
       argument++) {
    g_autofree gchar* route = nullptr;
    if (g_strcmp0(*argument, "--route") == 0 && argument[1] != nullptr) {
      route = route_from_value(argument[1]);
      argument++;
    } else {
      route = route_from_argument(*argument);
    }
    if (route != nullptr) {
      return g_steal_pointer(&route);
    }
  }
  return nullptr;
}

// Implements GApplication::activate.
static void my_application_activate(GApplication* application) {
  MyApplication* self = MY_APPLICATION(application);
  GList* windows = gtk_application_get_windows(GTK_APPLICATION(application));
  if (windows) {
    gtk_window_present(GTK_WINDOW(windows->data));
    return;
  }
  GtkWindow* window =
      GTK_WINDOW(gtk_application_window_new(GTK_APPLICATION(application)));

  // Use a header bar when running in GNOME as this is the common style used
  // by applications and is the setup most users will be using (e.g. Ubuntu
  // desktop).
  // If running on X and not using GNOME then just use a traditional title bar
  // in case the window manager does more exotic layout, e.g. tiling.
  // If running on Wayland assume the header bar will work (may need changing
  // if future cases occur).
  gboolean use_header_bar = FALSE;
#ifdef GDK_WINDOWING_X11
  GdkScreen* screen = gtk_window_get_screen(window);
  if (GDK_IS_X11_SCREEN(screen)) {
    const gchar* wm_name = gdk_x11_screen_get_window_manager_name(screen);
    if (g_strcmp0(wm_name, "GNOME Shell") != 0) {
      use_header_bar = FALSE;
    }
  }
#endif
  if (use_header_bar) {
    GtkHeaderBar* header_bar = GTK_HEADER_BAR(gtk_header_bar_new());
    gtk_widget_show(GTK_WIDGET(header_bar));
    gtk_header_bar_set_title(header_bar, "Discipulus");
    gtk_header_bar_set_show_close_button(header_bar, TRUE);
    gtk_window_set_titlebar(window, GTK_WIDGET(header_bar));
  } else {
    gtk_window_set_title(window, "Discipulus");
  }

  gtk_window_set_default_size(window, 1280, 720);
  gtk_widget_show(GTK_WIDGET(window));
  self->window = window;

  g_autoptr(FlDartProject) project = fl_dart_project_new();
  // Impeller loses its EGL context on this KDE Wayland session after login.
  // Use the stable Skia renderer for the Linux desktop build.
  fl_dart_project_set_enable_impeller(project, FALSE);
  fl_dart_project_set_dart_entrypoint_arguments(project, self->dart_entrypoint_arguments);

  FlView* view = fl_view_new(project);
  gtk_widget_show(GTK_WIDGET(view));
  gtk_container_add(GTK_CONTAINER(window), GTK_WIDGET(view));

  fl_register_plugins(FL_PLUGIN_REGISTRY(view));

  g_autoptr(FlStandardMethodCodec) codec = fl_standard_method_codec_new();
  self->desktop_channel = fl_method_channel_new(
      fl_engine_get_binary_messenger(fl_view_get_engine(view)),
      "dev.harrydekat.discipulus/desktop", FL_METHOD_CODEC(codec));
  if (self->pending_route != nullptr) {
    g_autofree gchar* route = g_steal_pointer(&self->pending_route);
    send_desktop_route(self, route);
  }

  gtk_widget_grab_focus(GTK_WIDGET(view));
}

// Handles command-line actions sent to the already-running application
// instance. This is what makes KDE Plasma task-manager actions useful even
// when Discipulus is already open.
static int my_application_command_line(
    GApplication* application,
    GApplicationCommandLine* command_line) {
  MyApplication* self = MY_APPLICATION(application);
  gint argument_count = 0;
  g_auto(GStrv) arguments =
      g_application_command_line_get_arguments(command_line, &argument_count);
  g_autofree gchar* route = route_from_arguments(arguments + 1);

  if (route != nullptr) {
    send_desktop_route(self, route);
  }
  if (self->window != nullptr) {
    gtk_window_present(self->window);
  } else {
    g_application_activate(application);
  }
  return 0;
}

// Handles URI launches from the desktop file's %U placeholder. GApplication
// forwards these to the primary process, so the same channel also covers a
// running window without starting a second Flutter engine.
static void my_application_open(GApplication* application,
                                GFile** files,
                                gint number_of_files,
                                const gchar* hint) {
  (void)hint;
  MyApplication* self = MY_APPLICATION(application);
  for (gint index = 0; index < number_of_files; index++) {
    g_autofree gchar* uri = g_file_get_uri(files[index]);
    g_autofree gchar* route = route_from_argument(uri);
    if (route != nullptr) {
      send_desktop_route(self, route);
    }
  }

  if (self->window != nullptr) {
    gtk_window_present(self->window);
  } else {
    g_application_activate(application);
  }
}

// Implements GApplication::local_command_line.
static gboolean my_application_local_command_line(GApplication* application, gchar*** arguments, int* exit_status) {
  MyApplication* self = MY_APPLICATION(application);
  // Strip out the first argument as it is the binary name.
  self->dart_entrypoint_arguments = g_strdupv(*arguments + 1);

  g_autoptr(GError) error = nullptr;
  if (!g_application_register(application, nullptr, &error)) {
     // A remote instance will receive the arguments through
     // my_application_command_line(). There is no local error in that case.
     if (error != nullptr) {
       g_warning("Failed to register: %s", error->message);
     }
     *exit_status = 0;
     return TRUE;
  }

  g_application_activate(application);
  *exit_status = 0;

  return FALSE;
}

// Implements GObject::dispose.
static void my_application_dispose(GObject* object) {
  MyApplication* self = MY_APPLICATION(object);
  g_clear_pointer(&self->dart_entrypoint_arguments, g_strfreev);
  g_clear_object(&self->desktop_channel);
  g_clear_pointer(&self->pending_route, g_free);
  G_OBJECT_CLASS(my_application_parent_class)->dispose(object);
}

static void my_application_class_init(MyApplicationClass* klass) {
  G_APPLICATION_CLASS(klass)->activate = my_application_activate;
  G_APPLICATION_CLASS(klass)->command_line = my_application_command_line;
  G_APPLICATION_CLASS(klass)->open = my_application_open;
  G_APPLICATION_CLASS(klass)->local_command_line = my_application_local_command_line;
  G_OBJECT_CLASS(klass)->dispose = my_application_dispose;
}

static void my_application_init(MyApplication* self) {
  self->window = nullptr;
  self->desktop_channel = nullptr;
  self->pending_route = nullptr;
}

MyApplication* my_application_new() {
  return MY_APPLICATION(g_object_new(my_application_get_type(),
                                     "application-id", APPLICATION_ID,
                                     "flags", G_APPLICATION_HANDLES_COMMAND_LINE | G_APPLICATION_HANDLES_OPEN,
                                     nullptr));
}
