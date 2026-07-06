package com.syncra.gestion_proyectos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.syncra.gestion_proyectos.entity.document.DocTemplateEntity;
import com.syncra.gestion_proyectos.repository.document.DocTemplateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
@Order(2)
public class DocTemplateSeeder implements CommandLineRunner {

    private final DocTemplateRepository docTemplateRepository;

    @Override
    public void run(String... args) {

        crear("PT-PP-01", "Planteamiento del Problema", "Vision general del problema y la solucion propuesta", 1L, contenidoPP01());
        crear("PT-ERS-02", "Especificacion de Requisitos", "Caracteristicas, requisitos funcionales y no funcionales del sistema", 2L, contenidoERS02());
        crear("PT-ECU-02", "Especificacion de Casos de Uso", "Interacciones entre usuarios y el sistema", 3L, contenidoECU02());
        crear("PT-IGS-01", "Informe General del Sistema", "Vision general del sistema, casos de uso e historias de usuario", 4L, contenidoIGS01());
        crear("PT-PS-01", "Prototipado del Sistema", "Prototipos graficos de baja o alta fidelidad", 5L, contenidoPS01());
        crear("PT-MCBD-01", "Manual de Configuracion BD", "Configuracion de la base de datos del proyecto", 6L, contenidoMCBD01());
        crear("PT-PP-03", "Plan de Pruebas", "Plan y casos de prueba del sistema", 7L, contenidoPP03());
        crear("PT-MTC-01", "Manual Tecnico de Configuracion", "Instalacion, configuracion y despliegue del software", 8L, contenidoMTC01());
        crear("PT-MU-01", "Manual de Usuario", "Explicacion de las funcionalidades del sistema por rol", 9L, contenidoMU01());
        crear("PT-AR-01", "Formato Acta de Reunion", "Registro de reuniones, compromisos y responsables", 10L, contenidoAR01());
        crear("BLANCO", "Documento en blanco", "Documento libre sin contenido predefinido", 11L, "<p></p>");
    }

    private void crear(String code, String title, String description, Long position, String defaultContent) {

        if (docTemplateRepository.findByCode(code).isPresent()) {
            return;
        }

        DocTemplateEntity template = new DocTemplateEntity();
        template.setCode(code);
        template.setTitle(title);
        template.setDescription(description);
        template.setPosition(position);
        template.setDefaultContent(defaultContent);

        docTemplateRepository.save(template);

        log.info("Plantilla creada por defecto: {}", code);
    }



    private String nota() {
        return """
            <p><em>Este y el resto de textos en cursiva se incluyen como guia para el diligenciamiento
            de este documento y deben ser eliminados antes de la entrega. El texto final debe ir sin
            cursiva, con el mismo tipo y tamano de letra, parrafos justificados; no se debe alterar la
            estructura ni el orden de las secciones sin autorizacion previa. Si una seccion no aplica,
            se debe indicar NA (No Aplica).</em></p>
            """;
    }

    private String historialRevision() {
        return """
            <h3>Historial de Revision</h3>
            <table>
              <tr><th>Version</th><th>Fecha Elaboracion</th><th>Responsable Elaboracion</th><th>Fecha Aprobacion</th><th>Responsable Aprobacion</th></tr>
              <tr><td>1.0</td><td></td><td></td><td></td><td></td></tr>
            </table>
            <h3>Cambios Respecto a la Version Anterior</h3>
            <table>
              <tr><th>Version</th><th>Modificacion respecto a version anterior</th></tr>
              <tr><td>1.0</td><td>Creacion del documento</td></tr>
            </table>
            <h3>Tabla de Contenido</h3>
            """;
    }

    private String contenidoPP01() {
        return """
            <h1>PT-PP-01. Planteamiento del Problema</h1>
            <p><em>Este documento permite dar una vision general del problema encontrado con el fin de
            determinar la solucion propuesta mediante el desarrollo de software.</em></p>
            """ + nota() + historialRevision() + """
            <h2>1. Introduccion</h2>
            <p><em>Introduccion corta sobre el contenido del documento y contextualizacion del proyecto.</em></p>
            <h3>1.1 Responsables e Involucrados</h3>
            <table>
              <tr><th>Nombre</th><th>Tipo (Responsable/Involucrado)</th><th>Rol</th><th>Cargo</th></tr>
              <tr><td></td><td></td><td></td><td></td></tr>
            </table>
            <h3>1.2 Referencias (Bibliografia o Web Grafia)</h3>
            <table>
              <tr><th>Nombre</th><th>Descripcion</th><th>Link Referencia</th></tr>
              <tr><td></td><td></td><td></td></tr>
            </table>
            <h2>2. Descripcion General</h2>
            <p><em>Explicar de forma general el problema identificado y contextualizar la solucion propuesta.</em></p>
            <h2>3. Situacion Actual</h2>
            <p><em>Explicar la situacion actual del contexto del problema.</em></p>
            <h2>4. Situacion Esperada</h2>
            <p><em>Explicar la situacion esperada con el desarrollo de la funcionalidad.</em></p>
            <h2>5. Justificacion</h2>
            <p><em>Justificar el porque de la eleccion de la solucion propuesta.</em></p>
            <h2>6. Aspectos Legales (normas o leyes)</h2>
            <table>
              <tr><th>Norma o Ley</th><th>Descripcion</th><th>Enlace</th></tr>
              <tr><td>Ley de Proteccion de Datos Personales (Ley 1581 de 2012)</td><td>Reconoce y protege el derecho a conocer, actualizar y rectificar la informacion recogida en bases de datos.</td><td></td></tr>
            </table>
            """;
    }

    private String contenidoERS02() {
        return """
            <h1>PT-ERS-02. Especificacion de Requisitos</h1>
            <p>Nombre del Proyecto</p><p>Numero de Version</p>
            <p><em>Este documento define el sistema en cuanto a caracteristicas, requisitos tecnicos,
            funcionales y no funcionales.</em></p>
            """ + nota() + historialRevision() + """
            <h2>1. Introduccion</h2>
            <p><em>Introduccion corta y contextualizacion del proyecto.</em></p>
            <h3>1.1 Responsables e Involucrados</h3>
            <table>
              <tr><th>Nombre</th><th>Tipo</th><th>Rol</th><th>Cargo</th></tr>
              <tr><td></td><td></td><td></td><td></td></tr>
            </table>
            <h3>1.2 Referencias (Bibliografia o Web Grafia)</h3>
            <table>
              <tr><th>Nombre</th><th>Descripcion</th><th>Link Referencia</th></tr>
              <tr><td></td><td></td><td></td></tr>
            </table>
            <h2>2. Caracteristicas del Producto</h2>
            <ul><li></li><li></li></ul>
            <h2>3. Caracteristicas del Usuario o Actores</h2>
            <table>
              <tr><th>Actor</th><th>Descripcion</th><th>Interacciones Esperadas</th></tr>
              <tr><td>Actor 1</td><td></td><td></td></tr>
            </table>
            <h2>4. Modulos y Funciones del Producto</h2>
            <table>
              <tr><th>ID</th><th>Modulo</th><th>Descripcion</th><th># RF</th></tr>
              <tr><td>M01</td><td></td><td></td><td></td></tr>
            </table>
            <h2>5. Especificacion de Requisitos</h2>
            <p><em>Criterio aplicado: Esencial (critico), Ideal (mejora la experiencia), Opcional (valor agregado).</em></p>
            <h3>5.1 Requisitos Funcionales</h3>
            <h4>5.1.1 Requisitos Funcionales M01 - Nombre modulo</h4>
            <table>
              <tr><th>ID</th><th>Nombre</th><th>Descripcion</th><th>Roles</th><th>Tipo</th></tr>
              <tr><td>RF1</td><td></td><td></td><td></td><td></td></tr>
            </table>
            <h3>5.2 Requisitos No Funcionales</h3>
            <table>
              <tr><th>Requisito</th><th>Descripcion</th></tr>
              <tr><td>Usabilidad</td><td></td></tr>
              <tr><td>Confiabilidad</td><td></td></tr>
              <tr><td>Seguridad</td><td></td></tr>
              <tr><td>Eficiencia y Rendimiento</td><td></td></tr>
              <tr><td>Portabilidad</td><td></td></tr>
              <tr><td>Mantenibilidad</td><td></td></tr>
              <tr><td>Soportabilidad</td><td></td></tr>
              <tr><td>Operatividad</td><td></td></tr>
            </table>
            <h2>6. Restricciones del Software</h2>
            <p><em>Problemas, restricciones o inconvenientes que puedan afectar el proyecto.</em></p>
            <h2>7. Anexos</h2>
            <table>
              <tr><th>Anexo</th><th>Descripcion</th></tr>
              <tr><td></td><td></td></tr>
            </table>
            """;
    }

    private String contenidoECU02() {
        return """
            <h1>PT-ECU-02. Especificacion de Casos de Uso del Sistema</h1>
            <p>Nombre del Proyecto</p><p>Numero de Version</p>
            <p><em>Este documento define los casos de uso del proyecto, describiendo las interacciones
            entre los usuarios y el sistema.</em></p>
            """ + nota() + historialRevision() + """
            <h2>1. Introduccion</h2>
            <p><em>Introduccion corta y contextualizacion del proyecto.</em></p>
            <h2>2. Descripcion General de Actores</h2>
            <table>
              <tr><th>Actor</th><th>Descripcion Detallada</th></tr>
              <tr><td>Actor 1</td><td></td></tr>
            </table>
            <h2>3. Diagrama de Casos de Uso</h2>
            <h3>3.1 General</h3>
            <p><em>Diagrama general de paquetes/modulos del sistema.</em></p>
            <h3>3.2 Especificos</h3>
            <p><em>Diagramas de subsistemas con sus respectivos include y extends.</em></p>
            <h4>3.2.1 Subsistema 1</h4>
            <h2>4. Especificacion de Casos de Uso</h2>
            <p><em>Tabla de especificacion por cada caso de uso resultante, ej: CU1 - Registrar Usuario.</em></p>
            <table>
              <tr><th>Campo</th><th>Descripcion</th></tr>
              <tr><td>Nombre del Caso de Uso</td><td></td></tr>
              <tr><td>Actores</td><td></td></tr>
              <tr><td>Precondiciones</td><td></td></tr>
              <tr><td>Flujo Principal</td><td></td></tr>
              <tr><td>Flujos Alternativos</td><td></td></tr>
              <tr><td>Postcondiciones</td><td></td></tr>
            </table>
            """;
    }

    private String contenidoIGS01() {
        return """
            <h1>PT-IGS-01. Informe General del Sistema</h1>
            <p>Nombre Proyecto o App</p>
            <p>Informe final del trabajo de grado</p>
            <p><em>Este documento permite dar una vision general del sistema a nivel de caracteristicas,
            funcionalidades, casos de uso e historias de usuario.</em></p>
            """ + nota() + """
            <p><strong>Nombre Proyecto</strong></p>
            <p><strong>Nombre Estudiante</strong></p>
            <p><strong>Centro de Formacion</strong></p>
            <p><strong>Titulacion</strong></p>
            <p><strong>Ciudad</strong></p>
            <p><strong>Ano</strong></p>
            <h2>Tabla de Contenido</h2>
            <h2>1. Introduccion</h2>
            <h2>2. Objetivo</h2>
            <p><em>Objetivo general del sistema.</em></p>
            <h2>3. Alcance</h2>
            <p><em>Hasta que punto llega el desarrollo del sistema.</em></p>
            <h2>4. Situacion Actual</h2>
            <h2>5. Situacion Esperada</h2>
            <h2>6. Justificacion</h2>
            <h2>7. Caracteristicas del Sistema</h2>
            <h2>8. Usuarios - Roles</h2>
            <h3>8.1 Usuario 1</h3>
            <h2>9. Diagrama de Casos de Uso</h2>
            <h3>9.1 General</h3>
            <h3>9.2 Especificos</h3>
            <h4>9.2.1 Subsistema 1</h4>
            <h2>10. Historias de Usuario</h2>
            <table>
              <tr><th>ID</th><th>Historia de Usuario</th><th>Criterios de Aceptacion</th></tr>
              <tr><td>HU1</td><td></td><td></td></tr>
            </table>
            """;
    }

    private String contenidoPS01() {
        return """
            <h1>PT-PS-01. Prototipado del Sistema</h1>
            <p>Nombre del Proyecto</p><p>Numero de Version</p>
            <p><em>Este documento aloja los prototipos graficos del software (baja o alta fidelidad),
            no corresponde a la maquetacion final en HTML5.</em></p>
            """ + nota() + historialRevision() + """
            <h2>1. Introduccion</h2>
            <h2>2. Arquitectura de Informacion</h2>
            <p><em>Estructura de navegacion del sistema.</em></p>
            <h2>3. Prototipos</h2>
            <h3>3.1 Prototipo 1</h3>
            <p><strong>Captura</strong></p>
            <p><strong>Descripcion</strong></p>
            <h3>3.2 Prototipo 2</h3>
            <p><strong>Captura</strong></p>
            <p><strong>Descripcion</strong></p>
            <h2>4. Enlace Prototipo Funcional</h2>
            <p><em>Enlace del prototipo funcional (Figma, Balsamiq, Mockplus, etc).</em></p>
            """;
    }

    private String contenidoMCBD01() {
        return """
            <h1>PT-MCBD-01. Manual de Configuracion BD</h1>
            <p>Nombre del Proyecto</p><p>Numero de Version</p>
            <p><em>Este documento especifica la configuracion de la o las bases de datos usadas en el
            proyecto formativo.</em></p>
            """ + nota() + historialRevision() + """
            <h2>1. Introduccion</h2>
            <h2>2. Alcance</h2>
            <h2>3. Modelo Entidad Relacion (MER)</h2>
            <h2>4. Diccionario de Datos</h2>
            <h2>5. Modelo Relacional o Estructura de Documentos</h2>
            <h2>6. Justificacion Sistema Gestor de Bases de Datos Seleccionado</h2>
            <h2>7. Requisitos de Configuracion</h2>
            <h2>8. Scripts</h2>
            <pre>CREATE TABLE nombre_tabla (
  columna1 tipo_dato,
  columna2 tipo_dato,
  PRIMARY KEY (columna1)
);</pre>
            <h2>9. Configuracion y Ejecucion de la Base de Datos</h2>
            <h2>10. Otras Consideraciones</h2>
            """;
    }

    private String contenidoPP03() {
        return """
            <h1>PT-PP-03. Plan de Pruebas</h1>
            <p><em>Este documento define y organiza el plan de pruebas del sistema desarrollado,
            estableciendo los procesos de validacion necesarios para verificar el correcto
            funcionamiento de los modulos, funcionalidades y requerimientos del proyecto.</em></p>
            """ + nota() + historialRevision() + """
            <h2>1. Introduccion</h2>
            <h2>2. Justificacion</h2>
            <h2>3. Objetivo</h2>
            <h2>4. Alcance</h2>
            <h2>5. Plan de Pruebas</h2>
            <table>
              <tr><th>ID</th><th>Modulo</th><th>Descripcion</th><th>Objetivo</th></tr>
              <tr><td>M01</td><td></td><td></td><td></td></tr>
            </table>
            <h2>6. Casos de Prueba del Sistema por Modulos</h2>
            <table>
              <tr><th>ID</th><th>Historia de Usuario</th><th>Escenario</th><th>Resultado Esperado</th><th>Resultado Obtenido</th><th>Estado</th></tr>
              <tr><td>CP01-HU1</td><td></td><td></td><td></td><td></td><td></td></tr>
            </table>
            """;
    }

    private String contenidoMTC01() {
        return """
            <h1>PT-MTC-01. Manual Tecnico de Configuracion</h1>
            <p>Nombre del Proyecto</p><p>Numero de Version</p>
            <p><em>Este documento especifica aspectos tecnicos de instalacion, configuracion o
            despliegue del software.</em></p>
            """ + nota() + historialRevision() + """
            <h2>1. Introduccion</h2>
            <h2>2. Alcance</h2>
            <h2>3. Definiciones, Siglas y Abreviaturas</h2>
            <h2>4. Aspectos Tecnicos</h2>
            <h3>4.1 Metodologia de Trabajo Usada</h3>
            <h3>4.2 Tipo de Software o Enfoque Tecnologico</h3>
            <h3>4.3 Arquitectura Tecnologica</h3>
            <h3>4.4 Patrones de Diseno Usados</h3>
            <h3>4.5 Gestion de Datos</h3>
            <h2>5. Requisitos de Configuracion</h2>
            <h2>6. Proceso de Configuracion o Despliegue</h2>
            <h2>7. Ingreso al Sistema</h2>
            <h2>8. Otras Consideraciones</h2>
            """;
    }

    private String contenidoMU01() {
        return """
            <h1>PT-MU-01. Manual de Usuario</h1>
            <p>Manual de Usuario &lt;Tipo Usuario&gt;</p>
            <p>Nombre del Proyecto</p><p>Numero de Version</p>
            <p><em>Se debe respetar el orden logico en la explicacion de las funcionalidades; si hay
            varios tipos de usuario, se recomienda un manual por cada uno.</em></p>
            """ + nota() + historialRevision() + """
            <h2>1. Introduccion</h2>
            <h2>2. Alcance</h2>
            <h2>3. Definiciones, Siglas y Abreviaturas</h2>
            <h2>4. Responsables e Involucrados</h2>
            <table>
              <tr><th>Nombre</th><th>Tipo</th><th>Rol</th></tr>
              <tr><td></td><td></td><td></td></tr>
            </table>
            <h2>5. Roles y Usuarios</h2>
            <h3>5.1 Usuarios</h3>
            <h3>5.2 Roles</h3>
            <h2>6. Ingreso al Sistema</h2>
            <h2>7. Navegacion</h2>
            <h2>8. Opciones, Modulos o Funcionalidades</h2>
            <h3>8.1 Opcion 1</h3>
            <h2>9. Mensajes</h2>
            <h3>9.1 Error</h3>
            <h3>9.2 Advertencia</h3>
            <h3>9.3 Confirmacion</h3>
            <h3>9.4 Informacion</h3>
            """;
    }

    private String contenidoAR01() {
        return """
            <h1>PT-AR-01. Formato Acta de Reunion</h1>
            <table>
              <tr><th>Fecha</th><td></td><th>Hora</th><td></td><th>Proyecto/Modulo</th><td></td></tr>
            </table>
            <table>
              <tr><th>Lugar</th><td></td></tr>
              <tr><th>Motivo de la Reunion</th><td></td></tr>
            </table>
            <p><strong>Participantes:</strong></p>
            <table>
              <tr><th>Nombre</th><th>Rol</th></tr>
              <tr><td></td><td></td></tr>
            </table>
            <h2>Orden del Dia</h2>
            <p><em>Indicar el objetivo de la reunion y describir el motivo, los temas que se trataran.</em></p>
            <h2>Desarrollo de la Reunion</h2>
            <p><em>Indicar como se desenvolvio la reunion, temas tratados o pendientes.</em></p>
            <h2>Compromisos</h2>
            <p><em>Actividades resultantes de la reunion y responsable de su ejecucion.</em></p>
            <table>
              <tr><th>Actividad</th><th>Responsable</th><th>Fecha Entrega</th><th>Estado (Asignado/En Proceso/Terminado/Detenido)</th></tr>
              <tr><td></td><td></td><td></td><td></td></tr>
            </table>
            """;
    }
}