# Fontes do relatório em PDF (opcional)

O relatório da obra funciona **sem nenhum arquivo aqui**. Quando esta pasta está
vazia, o `ProjectReportPdfRenderer` não registra fonte nenhuma e o renderer usa
as fontes Standard 14 do próprio PDF (Helvetica, via `sans-serif` no CSS). Elas
são codificadas em WinAnsi/CP1252, que cobre todo o português — `ã`, `ç`, `õ`,
`é`, `à`, `ê`, além de `—`, `²` e `·`, que o template usa.

## Quando vale a pena embutir uma fonte

As Standard 14 **não vão embutidas no PDF**: a aparência depende da fonte
substituta do visualizador, então métricas e desenho variam de máquina para
máquina. Além disso, qualquer caractere fora do WinAnsi (`→`, `✓`, `≥`, `₂`)
é trocado por `#` silenciosamente, com apenas um log de nível INFO.

Para tipografia estável e liberdade de caracteres, coloque dois arquivos aqui:

```
src/main/resources/fonts/NotoSans-Regular.ttf
src/main/resources/fonts/NotoSans-Bold.ttf
```

O renderer detecta os arquivos e passa a registrá-los automaticamente — não é
preciso mudar código nem CSS, porque o template já pede
`font-family: "Report Sans", sans-serif`.

Notas:

- Use uma fonte de licença permissiva (Noto Sans e Inter são OFL; Roboto é
  Apache 2.0). O nome do arquivo precisa ser exatamente o acima, ou ajuste as
  constantes `FONT_REGULAR`/`FONT_BOLD` no `ProjectReportPdfRenderer`.
- O arquivo **bold é registrado à parte de propósito**: o renderer não sintetiza
  negrito. Sem ele, os pesos 700 do template saem em peso normal.
- As fontes são registradas para `DOCUMENT` **e** `SVG`. O `SVG` só passa a
  importar se algum dia os gráficos ganharem `<text>` interno — hoje os rótulos
  são HTML sobreposto justamente para não depender disso.
- `subset = true`: só os glifos usados entram no PDF, então o arquivo final
  continua pequeno.
