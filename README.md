# Compagno virtuale

Piccola app Android nativa (Java) per Android 8 e successivi, pensata e verificata nel progetto per Android 13 (API 33). Disegna un pupazzetto direttamente in una finestra `TYPE_APPLICATION_OVERLAY`: non è una pagina web, non usa root e non richiede librerie o servizi online.

## Funzioni

- Pupazzetto sempre in primo piano sopra le altre app, finché il servizio è attivo.
- Camminata autonoma lenta, con inversione quando raggiunge i bordi.
- Tocchi per salutare, saltellare o sedersi; pressione prolungata per farlo dormire.
- Trascinamento con il dito e nuvolette con messaggi in italiano, a intervalli rilassati.
- Notifica persistente con comando **Nascondi** per fermare il servizio.
- Nessun accesso a microfono, rete, posizione o contatti. Il microfono e la voce naturale sono lasciati a una versione futura.

Il servizio usa una singola vista leggera e aggiorna l'animazione circa 8 volte al secondo. Android può comunque sospendere o terminare servizi in base alle impostazioni energetiche del produttore. Se ColorOS interrompe il pupazzo, consenti l'attività in background per l'app nelle impostazioni batteria di Realme UI.

## Compilare l'APK

Occorrono JDK 17, Android SDK Platform 35 e Android Gradle Plugin 8.7.3; Gradle 8.9 o successivo è consigliato. Il progetto non include un wrapper Gradle perché in questa sessione Termux non dispone di Java/Gradle e il suo gestore pacchetti `apt` non è utilizzabile.

Con Android Studio: apri la cartella `CompagnoOverlay`, lascia che Gradle sincronizzi il progetto e scegli **Build > Build APK(s)**. L'APK di debug verrà creato in `app/build/outputs/apk/debug/app-debug.apk`.

Da un terminale dove `gradle` e l'SDK sono già configurati, esegui:

```sh
./build.sh
```

Oppure:

```sh
gradle --no-daemon :app:assembleDebug
```

## Compilazione automatica con GitHub Actions

Il workflow `.github/workflows/build-debug-apk.yml` compila l'APK debug su Ubuntu 24.04 con Temurin JDK 17, Android SDK Platform 35 e Gradle 8.9. Parte quando pubblichi un commit su `main` o `master`, per le pull request verso questi rami, oppure manualmente da GitHub Actions. Al termine pubblica il singolo file `app-debug.apk` come artifact scaricabile, con conservazione di 14 giorni.

Per usarlo, crea un repository GitHub e carica **il contenuto di questa cartella** come radice del repository, includendo la cartella nascosta `.github`. Nella scheda **Actions**, apri l'esecuzione completata di **Build Android debug APK** e scarica l'artifact **app-debug.apk**. Con la modalità file singolo abilitata, il download è l'APK stesso. L'esecuzione manuale si avvia da **Actions > Build Android debug APK > Run workflow**.

Trasferisci l'APK sul Realme e aprilo per installarlo; poi abilita **Mostra sopra altre app** quando l'app te lo chiede.

## Installare sul Realme

1. Copia `app/build/outputs/apk/debug/app-debug.apk` sul telefono e aprilo da File.
2. Se richiesto, abilita **Installa app sconosciute** per l'app usata per aprire il file, poi completa l'installazione.
3. Apri **Compagno virtuale** e premi **Fai comparire il pupazzo**.
4. Nella schermata Android **Mostra sopra altre app**, abilita l'autorizzazione e torna all'app; se necessario premi di nuovo il pulsante.
5. Per chiuderlo, usa **Metti via il pupazzo** nell'app oppure **Nascondi** nella notifica persistente.

Android 13 può richiedere l'autorizzazione notifiche dopo l'installazione. Se non concedi le notifiche, il sistema continua a gestire il servizio in primo piano, ma la notifica potrebbe non comparire nel pannello standard.

## Struttura

- `app/src/main/java/it/compagnovirtuale/app/MainActivity.java`: schermata di controllo e richiesta del permesso overlay.
- `app/src/main/java/it/compagnovirtuale/app/PetOverlayService.java`: servizio in primo piano, finestra overlay, disegno e animazioni.
- `app/src/main/AndroidManifest.xml`: permessi Android e dichiarazione del servizio.
