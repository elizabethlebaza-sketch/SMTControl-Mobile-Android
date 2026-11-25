package com.example.smtcontrolmobile;

import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.json.JSONArray;

public class MainActivity extends AppCompatActivity {

    String API = "https://smtcontrol-backend.onrender.com";
    LinearLayout layout;
    EditText correo, clave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarLogin();
    }

    void prepararPantalla(String titulo) {
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 35, 30, 25);
        layout.setBackgroundColor(Color.rgb(242, 246, 250));

        TextView encabezado = new TextView(this);
        encabezado.setText(titulo);
        encabezado.setTextSize(25);
        encabezado.setTextColor(Color.rgb(15, 55, 100));
        encabezado.setGravity(Gravity.CENTER);
        encabezado.setPadding(5, 25, 5, 30);
        layout.addView(encabezado);

        setContentView(layout);
    }

    void agregarTexto(String texto) {
        TextView t = new TextView(this);
        t.setText(texto);
        t.setTextSize(16);
        t.setTextColor(Color.DKGRAY);
        t.setPadding(10, 15, 10, 15);
        layout.addView(t);
    }

    void agregarBoton(String texto, Runnable accion) {
        Button b = new Button(this);
        b.setText(texto);
        layout.addView(b);
        b.setOnClickListener(v -> accion.run());
    }

    void mostrarLogin() {
        prepararPantalla("SMTCONTROL MOBILE");
        agregarTexto("Sistema de control de visitantes y seguridad");
        correo = new EditText(this);
        correo.setHint("Correo electrónico");
        correo.setInputType(33);
        layout.addView(correo);

        clave = new EditText(this);
        clave.setHint("Contraseña");
        clave.setInputType(129);
        layout.addView(clave);

        agregarBoton("INICIAR SESIÓN", this::iniciarSesion);
        agregarTexto("Aplicación móvil de administración y control de acceso.");
    }

    void iniciarSesion() {
        String email = correo.getText().toString().trim();
        String password = clave.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(API + "/usuarios/login");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);
                con.setConnectTimeout(15000);
                con.setReadTimeout(15000);

                JSONObject datos = new JSONObject();
                datos.put("correo", email);
                datos.put("contrasena", password);

                try (OutputStream os = con.getOutputStream()) {
                    os.write(datos.toString().getBytes(StandardCharsets.UTF_8));
                }

                int codigo = con.getResponseCode();
                InputStream is = codigo >= 200 && codigo < 300
                        ? con.getInputStream() : con.getErrorStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder respuesta = new StringBuilder();
                String linea;
                while ((linea = br.readLine()) != null) {
                    respuesta.append(linea);
                }
                con.disconnect();

                JSONObject resultado = new JSONObject(respuesta.toString());

                runOnUiThread(() -> {
                    if (codigo >= 200 && codigo < 300) {
                        String rol = resultado.optString("rol", "");
                        if (rol.isEmpty()) rol = "ADMINISTRADOR";
                        mostrarMenu(rol);
                    } else {
                        Toast.makeText(this, "Credenciales incorrectas o error de acceso",
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error de conexión: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    void mostrarMenu(String rol) {
        prepararPantalla("SMTCONTROL MOBILE");
        agregarTexto("Bienvenido | Rol: " + rol);

        if (rol.equalsIgnoreCase("ADMINISTRADOR")) {
            agregarBoton("USUARIOS", () -> consultar("/usuarios/listar"));
            agregarBoton("CLIENTES", () -> consultar("/clientes/listar"));
            agregarBoton("VISITANTES", () -> consultar("/visitantes/listar"));
            agregarBoton("INGRESOS", () -> consultar("/ingresos/listar"));
            agregarBoton("RESERVAS", () -> consultar("/reservas/listar"));
            agregarBoton("ÁREAS COMUNES", () -> consultar("/areas-comunes/listar"));
        } else if (rol.equalsIgnoreCase("VIGILANTE")) {
            agregarBoton("VISITANTES", () -> consultar("/visitantes/listar"));
            agregarBoton("INGRESOS", () -> consultar("/ingresos/listar"));
        } else if (rol.equalsIgnoreCase("RESIDENTE")) {
            agregarBoton("RESERVAS", () -> consultar("/reservas/listar"));
        }

        agregarBoton("CERRAR SESIÓN", this::mostrarLogin);
    }

    void consultar(String endpoint) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(API + endpoint);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(15000);
                con.setReadTimeout(15000);

                int codigo = con.getResponseCode();
                InputStream is = codigo >= 200 && codigo < 300
                        ? con.getInputStream() : con.getErrorStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder respuesta = new StringBuilder();
                String linea;
                while ((linea = br.readLine()) != null) {
                    respuesta.append(linea).append("\n");
                }
                con.disconnect();

                String resultado = respuesta.toString();

                runOnUiThread(() -> mostrarDatos(endpoint, resultado, codigo));

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "No se pudo consultar el servicio",
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    void mostrarDatos(String modulo, String datos, int codigo) {
        prepararPantalla("SMTCONTROL MOBILE");

        agregarTexto("MÓDULO: " + modulo.toUpperCase());
        agregarTexto("Código de respuesta: " + codigo);

        if (codigo < 200 || codigo >= 300) {
            agregarTexto("No fue posible consultar los registros.");
            agregarTexto(datos);
            agregarBoton("VOLVER AL MENÚ",
                    () -> mostrarMenu("ADMINISTRADOR"));
            return;
        }

        try {
            JSONArray registros = new JSONArray(datos);

            agregarTexto("Registros encontrados: " + registros.length());

            ScrollView scroll = new ScrollView(this);
            LinearLayout lista = new LinearLayout(this);
            lista.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(lista);

            layout.addView(scroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

            for (int i = 0; i < registros.length(); i++) {
                JSONObject registro = registros.getJSONObject(i);

                TextView tarjeta = new TextView(this);
                StringBuilder contenido = new StringBuilder();

                contenido.append("REGISTRO ").append(i + 1).append("\n\n");

                java.util.Iterator<String> claves = registro.keys();

                while (claves.hasNext()) {
                    String clave = claves.next();

                    if (clave.toLowerCase().contains("password")
                            || clave.toLowerCase().contains("contrasena")) {
                        continue;
                    }

                    contenido.append(clave.toUpperCase())
                            .append(": ")
                            .append(registro.optString(clave))
                            .append("\n");
                }

                tarjeta.setText(contenido.toString());
                tarjeta.setTextSize(14);
                tarjeta.setTextColor(Color.DKGRAY);
                tarjeta.setPadding(20, 20, 20, 20);
                tarjeta.setBackgroundColor(Color.WHITE);

                LinearLayout.LayoutParams parametros =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);

                parametros.setMargins(5, 8, 5, 8);
                lista.addView(tarjeta, parametros);
            }

        } catch (Exception e) {
            agregarTexto("Respuesta recibida del servidor:");
            agregarTexto(datos);
        }

        agregarBoton("VOLVER AL MENÚ",
                () -> mostrarMenu("ADMINISTRADOR"));
    }
}