# Spotify Liquid Glass (uso pessoal)

Modulo Xposed (API legada) que desenha a barra do Spotify com a biblioteca
Backdrop / AndroidLiquidGlass (Kyant0, Apache-2.0 - ver LICENSE-AndroidLiquidGlass).

Como funciona: a barra nativa `navigation_bar` fica invisivel (so mantem layout e cliques) e uma
ComposeView com LiquidBottomTabs e desenhada por cima. A cada layout o modulo revalida a barra,
entao ele continua funcionando quando o Spotify a recria (Criar, configuracoes).

## Compilar
GitHub: envie os arquivos -> Actions -> Build -> Run workflow -> baixe SpotifyLiquidGlass-apk.

## Usar sem root
NPatch -> Patch no Spotify original -> embutir SOMENTE este modulo -> instalar o APK gerado.

## Depurar
adb logcat -s SpotifyGlass
Esperado: "carregado no Spotify", "encontrado: navigation_bar" e "encontrado: now_playing_bar".
Se aparecer varias linhas "dump", o modulo nao achou os itens clicaveis da barra:
mande essas linhas para ajustar findTabs().
