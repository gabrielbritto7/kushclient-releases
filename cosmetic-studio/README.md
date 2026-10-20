# KUSH Cosmetic Studio 2.0

Aplicativo desktop offline para criar cosméticos, importar GLB ou MODEL.JSON, ajustar a peça em um personagem de Minecraft e gerar uma cópia do KUSHMOD com o novo item.

## Usar no Windows

Baixe o artefato **KUSH-Cosmetic-Studio-Windows-x64** na execução aprovada em GitHub Actions, extraia o ZIP e abra **KUSH-Cosmetic-Studio-2.0.0-Windows-x64.exe**. É uma versão portátil para Windows x64. Não exige Node, navegador aberto, Java ou instalação para editar. O Minecraft e o KUSHMOD continuam necessários para usar o cosmético no jogo.

1. Importe um **GLB** ou **MODEL.JSON**, ou comece com um chapéu, mochila ou asas.
2. Para JSON, selecione também sua **TEXTURE.PNG**.
3. Para GLB, escolha o detalhe e clique em **Converter para o KUSHMOD**.
4. Escolha cabeça, rosto, costas, peito, braço ou perna. Arraste as setas para mover; use **Girar** ou os campos numéricos. O controle de tamanho mantém a proporção.
5. Informe nome, ID único e aba do cosmético.
6. Selecione o KUSHMOD original e clique em **Gerar KUSHMOD com cosmético**.

O arquivo original não é sobrescrito automaticamente: uma janela pede onde salvar a cópia. Feche o Minecraft antes de substituir seu mod pela cópia gerada. **Exportar MODEL + CATALOG + TEXTURE** produz um ZIP com os três arquivos e um catalog-item.json para integração manual.

## Modelagem e projetos

Crie, duplique, exclua e redimensione cubos. Selecione na lista ou clique na peça na cena. Os modelos iniciados pelos botões do Studio permitem trocar as cores dos cubos. JSONs e GLBs preservam sua textura, sem editor de pintura UV nesta versão. **Desfazer** também funciona com Ctrl+Z. Salve em **.kushstudio** para continuar depois; o projeto inclui geometria, textura, nome, aba e ajuste, sem incluir o JAR do mod.

A skin de referência pode ser substituída por um PNG clássico de 64×64 ou 64×32. O personagem usa braços clássicos de 4 pixels. A prévia mostra a pose em pé; a movimentação do personagem e a compatibilidade final devem ser conferidas no Minecraft.

## Conversão GLB

O renderizador atual do KUSHMOD aceita cubos, não malhas de triângulos arbitrárias. Por isso a conversão usa voxelização da superfície e grava a cor base e a textura em um atlas PNG. Curvas, detalhes pequenos, transparência parcial e materiais PBR podem mudar. O resultado convertido é mostrado antes da exportação, e pode ser comparado com o GLB original.

- glTF 2.0 binário, até 64 MB, com buffers e texturas incorporados.
- Inclui decodificadores locais Draco, Meshopt e Basis/KTX2.
- Até 200 mil triângulos e 2.048 cubos no resultado; reduza o detalhe se o limite for excedido.
- Usa a pose estática; não exporta animações, rig, iluminação, metalicidade ou normal maps.
- Não faz download de modelos e não carrega recursos externos.
- Modelos instanciados por EXT_mesh_gpu_instancing devem ser convertidos para malhas comuns antes da importação.

## Compatibilidade do KUSHMOD

Mantém o montador de catálogo do Studio 1.0. O JAR precisa ser Fabric, id fastclient-hud, nome KUSH, catálogo assets/kush/cosmetics/catalog.json com schema 1 e KushCatalogProvider compatível com caminhos builtin/. Outros formatos de mod são recusados.

O ajuste é gravado no modelo, com coordenadas do personagem, grupos de rotação ZYX e flag PLAYER_SPACE (512). Os itens exportados usam flags 525 e uma fixação individual. Os testes verificam a igualdade geométrica entre a prévia e o JSON exportado, preservação dos arquivos existentes no JAR, e importação GLB → conversão → ajuste → exportação no aplicativo Windows. Testes com JAR sintético não substituem uma execução no Minecraft.

## Desenvolvimento

Requer Node 24 e npm.

~~~sh
cd cosmetic-studio
npm install
npm test
npm run build
npm run test:ui
npm start
~~~

Build Windows em um computador Windows:

~~~sh
npm run dist:win
~~~

O workflow .github/workflows/cosmetic-studio-windows.yml executa testes, empacota o aplicativo portátil, testa o aplicativo empacotado e publica o EXE, SHA256, este guia e o relatório de validação como artefato. O workflow não altera as releases do launcher nem publica em Vercel.

A pasta cosmetic-studio contém o código do Studio; os arquivos e releases existentes do repositório são independentes. Dependências de terceiros mantêm suas licenças e seus avisos acompanham o aplicativo. Este projeto não é afiliado à Mojang ou Microsoft.
