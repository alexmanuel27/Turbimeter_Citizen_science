/*
  ============================================================
   SENSOR DE TURBIDEZ DFROBOT SEN0189 + ARDUINO NANO
   VERSION 1: CALCULO EN NTU + CALIBRACION INTERACTIVA POR SERIAL
  ============================================================
  Descripcion:
   Lee el sensor de turbidez, calcula un valor aproximado en NTU
   usando una formula polinomica, y enciende un LED segun el
   nivel detectado. Incluye un asistente de calibracion manual
   que se maneja escribiendo letras en el Monitor Serial.

  *** ADVERTENCIA ***
   La formula NTU usada es una aproximacion muy usada por la
   comunidad de Arduino, NO es una ecuacion oficial publicada
   por DFRobot (ellos solo publican una grafica de referencia).
   Por eso este codigo calibra los UMBRALES de los LEDs con tus
   propias muestras reales, en vez de confiar ciegamente en la
   formula.

  Conexion del sensor: 
   - Cable ROJO   -> 5V
   - Cable NEGRO  -> GND
   - Cable AZUL   -> Pin analogico A1 (señal)

  LIBRERIAS NECESARIAS:
   - Ninguna libreria externa para el sensor (solo analogRead).
   - <EEPROM.h> se usa para guardar la calibracion de forma
     permanente. Es una libreria NATIVA de Arduino, ya viene
     instalada con el IDE, no hay que descargar nada.

  ============================================================
                 GUIA DE CALIBRACION (Monitor Serial)
  ============================================================
   1. Abre el Monitor Serial (Ctrl+Shift+M) a 9600 baudios.
   2. Escribe la letra  C  y presiona Enter -> inicia calibracion.
   3. El programa te pedira sumergir el sensor en AGUA CLARA (pura).
      Espera a que el valor se estabilice y escribe  F  + Enter
      para fijar ese punto.
   4. Luego te pedira AGUA CON TINTE AMARILLO (turbidez media).
      Sumerge el sensor, espera que se estabilice y escribe
      F  + Enter para fijar ese punto.
   5. Luego te pedira AGUA CON CAFE DISUELTO O TINTA OSCURA
      (turbidez alta). Sumerge el sensor, espera que se
      estabilice y escribe  F  + Enter para fijar ese punto.
   6. Finalmente escribe  X  + Enter para FINALIZAR la
      calibracion. Los valores se calculan y se guardan en la
      memoria EEPROM automaticamente.

   Sugerencia para la muestra "turbidez alta": disuelve un poco
   de cafe instantaneo en agua, o usa tinta china diluida. Evita
   sustancias toxicas o corrosivas, ya que el sensor quedara
   sumergido varios segundos.
  ============================================================
*/

#include <EEPROM.h> // Libreria nativa de Arduino para guardar datos permanentes

// ------------------- CONFIGURACION DE PINES -------------------

const int PIN_SENSOR_TURBIDEZ = A1; // Cable azul (señal) del sensor

// >>> AQUI DEBES COLOCAR LOS PINES DIGITALES QUE VAYAS A USAR <<<
const int PIN_LED_ROJO     = 8;   // <-- CAMBIAR: pin digital del LED ROJO
const int PIN_LED_AMARILLO = 9;   // <-- CAMBIAR: pin digital del LED AMARILLO
const int PIN_LED_VERDE    = 10;  // <-- CAMBIAR: pin digital del LED VERDE

const int NUMERO_DE_MUESTRAS = 800; // Lecturas para promediar y reducir ruido

// ------------------- CONSTANTES DE LA FORMULA NTU -------------------
const float VOLTAJE_REFERENCIA_ARDUINO = 5.00; // <-- AJUSTAR si tu 5V real es distinto
float COEF_A = -1120.4; // <-- AJUSTAR solo si haces tu propia regresion con NTU reales
float COEF_B = 5742.3;  // <-- AJUSTAR solo si haces tu propia regresion con NTU reales
float COEF_C = -4352.9; // <-- AJUSTAR solo si haces tu propia regresion con NTU reales

// ------------------- DIRECCIONES DE MEMORIA EEPROM -------------------
// Cada float ocupa 4 bytes, por eso las direcciones van de 4 en 4
const int DIRECCION_NTU_CLARA   = 0;
const int DIRECCION_NTU_MEDIA   = 4;
const int DIRECCION_NTU_TURBIA  = 8;
const int DIRECCION_CALIBRADO   = 12; // Bandera: 1 = ya se calibro antes

// ------------------- VARIABLES DE CALIBRACION (EN NTU) -------------------
// Estos valores se calculan al calibrar, o se cargan desde la EEPROM
// si ya se habia calibrado antes. Sirven como umbrales para los LEDs.
float ntuAguaClara  = 200.0;  // Valor por defecto hasta que se calibre
float ntuAguaMedia  = 1000.0; // Valor por defecto hasta que se calibre
float ntuAguaTurbia = 2500.0; // Valor por defecto hasta que se calibre

float UMBRAL_TURBIDEZ_ALTA;  // Se calcula automaticamente entre media y turbia
float UMBRAL_TURBIDEZ_MEDIA; // Se calcula automaticamente entre clara y media

// ------------------- VARIABLES DE ESTADO DE CALIBRACION -------------------
bool enCalibracion = false;   // true mientras el asistente esta activo
int  pasoCalibracion = 0;     // 0=inactivo, 1=agua clara, 2=agua media, 3=agua turbia


void setup() {
  Serial.begin(9600);

  pinMode(PIN_LED_ROJO, OUTPUT);
  pinMode(PIN_LED_AMARILLO, OUTPUT);
  pinMode(PIN_LED_VERDE, OUTPUT);
  apagarTodosLosLeds();

  cargarCalibracionDesdeEEPROM();
  recalcularUmbrales();

  Serial.println("============================================");
  Serial.println(" Sensor de turbidez DFRobot - Modo NTU");
  Serial.println(" Escribe 'C' + Enter para iniciar calibracion");
  Serial.println("============================================");
}


void loop() {

  // ---------- Revisar si el usuario escribio algo en el Monitor Serial ----------
  if (Serial.available() > 0) {
    char teclaRecibida = Serial.read();
    procesarTecla(teclaRecibida);
  }

  // ---------- PASO 1: LEER EL SENSOR VARIAS VECES ----------
  long sumaLecturas = 0;
  for (int i = 0; i < NUMERO_DE_MUESTRAS; i++) {
    sumaLecturas += analogRead(PIN_SENSOR_TURBIDEZ);
    delayMicroseconds(500);
  }
  float promedioLectura = sumaLecturas / (float)NUMERO_DE_MUESTRAS;

  // ---------- PASO 2: CONVERTIR A VOLTAJE ----------
  float voltaje = promedioLectura * (VOLTAJE_REFERENCIA_ARDUINO / 1023.0);

  // ---------- PASO 3: CONVERTIR A NTU ----------
  float ntu = calcularNTU(voltaje);

  // ---------- PASO 4: MOSTRAR LECTURA EN EL MONITOR SERIAL ----------
  Serial.print("Voltaje: ");
  Serial.print(voltaje, 3);
  Serial.print(" V  |  NTU estimado: ");
  Serial.print(ntu, 1);

  if (enCalibracion) {
    // Mientras se calibra, no se evaluan los LEDs, solo se muestra la lectura
    Serial.print("   [CALIBRANDO - Paso ");
    Serial.print(pasoCalibracion);
    Serial.println(" de 3]");
  } else {
    // ---------- PASO 5: EVALUAR LOS 3 CONDICIONALES DE LOS LEDS ----------
    Serial.print("  |  Estado: ");
    evaluarLeds(ntu);
  }

  delay(500); // Actualizacion cada medio segundo
}


// ============================================================
//                   FUNCIONES DE CALIBRACION
// ============================================================

void procesarTecla(char tecla) {

  if (tecla == 'C' || tecla == 'c') {
    // ----- Iniciar calibracion -----
    enCalibracion = true;
    pasoCalibracion = 1;
    Serial.println();
    Serial.println("### CALIBRACION INICIADA ###");
    Serial.println("Paso 1/3: Sumerge el sensor en AGUA CLARA (pura).");
    Serial.println("Cuando la lectura se estabilice, escribe 'F' + Enter.");
  }
  else if ((tecla == 'F' || tecla == 'f') && enCalibracion) {
    // ----- Fijar el valor actual segun el paso -----
    float voltajeActual = leerVoltajePromedio();
    float ntuActual = calcularNTU(voltajeActual);

    if (pasoCalibracion == 1) {
      ntuAguaClara = ntuActual;
      Serial.print("Valor fijado para AGUA CLARA: ");
      Serial.print(ntuActual, 1);
      Serial.println(" NTU");
      Serial.println("Paso 2/3: Sumerge el sensor en AGUA CON TINTE AMARILLO.");
      Serial.println("Cuando la lectura se estabilice, escribe 'F' + Enter.");
      pasoCalibracion = 2;
    }
    else if (pasoCalibracion == 2) {
      ntuAguaMedia = ntuActual;
      Serial.print("Valor fijado para AGUA MEDIA (amarilla): ");
      Serial.print(ntuActual, 1);
      Serial.println(" NTU");
      Serial.println("Paso 3/3: Sumerge el sensor en AGUA CON CAFE/TINTA OSCURA.");
      Serial.println("Cuando la lectura se estabilice, escribe 'F' + Enter.");
      pasoCalibracion = 3;
    }
    else if (pasoCalibracion == 3) {
      ntuAguaTurbia = ntuActual;
      Serial.print("Valor fijado para AGUA TURBIA (oscura): ");
      Serial.print(ntuActual, 1);
      Serial.println(" NTU");
      Serial.println("Los 3 puntos ya fueron fijados.");
      Serial.println("Escribe 'X' + Enter para FINALIZAR y guardar la calibracion.");
      pasoCalibracion = 4; // Esperando finalizacion
    }
    else {
      Serial.println("Ya fijaste los 3 puntos. Escribe 'X' para finalizar.");
    }
  }
  else if ((tecla == 'X' || tecla == 'x') && enCalibracion) {
    // ----- Finalizar calibracion -----
    if (pasoCalibracion < 4) {
      Serial.println("Aun no has fijado los 3 puntos con 'F'. No se puede finalizar todavia.");
    } else {
      recalcularUmbrales();
      guardarCalibracionEnEEPROM();
      enCalibracion = false;
      pasoCalibracion = 0;
      Serial.println("### CALIBRACION FINALIZADA Y GUARDADA EN MEMORIA ###");
      Serial.println("Volviendo al funcionamiento normal...");
      Serial.println();
    }
  }
}

// Lee el sensor varias veces y devuelve el voltaje promedio (usado durante calibracion)
float leerVoltajePromedio() {
  long sumaLecturas = 0;
  for (int i = 0; i < NUMERO_DE_MUESTRAS; i++) {
    sumaLecturas += analogRead(PIN_SENSOR_TURBIDEZ);
    delayMicroseconds(500);
  }
  float promedioLectura = sumaLecturas / (float)NUMERO_DE_MUESTRAS;
  return promedioLectura * (VOLTAJE_REFERENCIA_ARDUINO / 1023.0);
}

// Convierte un voltaje a NTU usando la formula polinomica
float calcularNTU(float voltaje) {
  float ntu;
  if (voltaje < 2.5) {
    ntu = 3000;
  } else {
    ntu = (COEF_A * voltaje * voltaje) + (COEF_B * voltaje) + COEF_C;
  }
  if (ntu < 0) ntu = 0;
  return ntu;
}

// Calcula los umbrales de los LEDs como puntos intermedios entre las 3 muestras calibradas
void recalcularUmbrales() {
  UMBRAL_TURBIDEZ_MEDIA = (ntuAguaClara + ntuAguaMedia) / 2.0;
  UMBRAL_TURBIDEZ_ALTA  = (ntuAguaMedia + ntuAguaTurbia) / 2.0;
}

// Guarda los 3 valores calibrados de forma permanente en la EEPROM
void guardarCalibracionEnEEPROM() {
  EEPROM.put(DIRECCION_NTU_CLARA, ntuAguaClara);
  EEPROM.put(DIRECCION_NTU_MEDIA, ntuAguaMedia);
  EEPROM.put(DIRECCION_NTU_TURBIA, ntuAguaTurbia);
  EEPROM.put(DIRECCION_CALIBRADO, (byte)1); // Marca que ya existe una calibracion guardada
}

// Carga la calibracion guardada anteriormente (si existe) al encender el Arduino
void cargarCalibracionDesdeEEPROM() {
  byte banderaCalibrado;
  EEPROM.get(DIRECCION_CALIBRADO, banderaCalibrado);

  if (banderaCalibrado == 1) {
    EEPROM.get(DIRECCION_NTU_CLARA, ntuAguaClara);
    EEPROM.get(DIRECCION_NTU_MEDIA, ntuAguaMedia);
    EEPROM.get(DIRECCION_NTU_TURBIA, ntuAguaTurbia);
    Serial.println("Se cargo una calibracion guardada previamente.");
  } else {
    Serial.println("No hay calibracion guardada. Se usaran valores por defecto.");
  }
}

// ============================================================
//                     FUNCIONES DE LOS LEDS
// ============================================================

void apagarTodosLosLeds() {
  digitalWrite(PIN_LED_ROJO, LOW);
  digitalWrite(PIN_LED_AMARILLO, LOW);
  digitalWrite(PIN_LED_VERDE, LOW);
}

// Evalua los 3 condicionales y enciende el LED correspondiente
void evaluarLeds(float ntu) {
  if (ntu > UMBRAL_TURBIDEZ_ALTA) {
    digitalWrite(PIN_LED_ROJO, HIGH);
    digitalWrite(PIN_LED_AMARILLO, LOW);
    digitalWrite(PIN_LED_VERDE, LOW);
    Serial.println("TURBIDEZ ALTA - LED ROJO");
  }
  else if (ntu > UMBRAL_TURBIDEZ_MEDIA) {
    digitalWrite(PIN_LED_ROJO, LOW);
    digitalWrite(PIN_LED_AMARILLO, HIGH);
    digitalWrite(PIN_LED_VERDE, LOW);
    Serial.println("TURBIDEZ MEDIA - LED AMARILLO");
  }
  else {
    digitalWrite(PIN_LED_ROJO, LOW);
    digitalWrite(PIN_LED_AMARILLO, LOW);
    digitalWrite(PIN_LED_VERDE, HIGH);
    Serial.println("TURBIDEZ BAJA - LED VERDE");
  }
}
