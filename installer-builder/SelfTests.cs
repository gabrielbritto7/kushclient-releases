using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Drawing;
using System.Windows.Forms;

namespace KushBuilder {
    public static class SelfTests {
        static void ExpectFailure(Action action, string label) {
            bool failed = false; try { action(); } catch (InvalidOperationException) { failed = true; }
            Packager.Require(failed, "A validação deixou passar: " + label);
        }
        public static int Run() {
            var results = new List<string>();
            string root = Path.Combine(Packager.AppRoot, "test-output"), fixture = Path.Combine(root, "pasta com acentuação $", "KushClient");
            try {
                Directory.CreateDirectory(Path.Combine(fixture, "assets")); Directory.CreateDirectory(Path.Combine(fixture, "runtime"));
                Directory.CreateDirectory(Path.Combine(root, "saida"));
                string program = System.Reflection.Assembly.GetExecutingAssembly().Location;
                File.Copy(program, Path.Combine(fixture, "KushClient.exe"), true);
                File.Copy(program, Path.Combine(fixture, "runtime/pythonw.exe"), true);
                File.Copy(Path.Combine(Packager.AppRoot, "kushclient.ico"), Path.Combine(fixture, "assets/kushclient.ico"), true);
                File.Copy(Path.Combine(Packager.AppRoot, "test-support/installer_model.py"), Path.Combine(fixture, "installer_model.py"), true);
                File.WriteAllText(Path.Combine(fixture, "assets/dolar$-ação.txt"), "Arquivo de teste com acentos e cifrão.");
                File.WriteAllText(Path.Combine(fixture, "launcher.py"), "print('Fixture de empacotamento')\n");
                File.WriteAllText(Path.Combine(fixture, "core.py"), "VERSION = '0.9.17'\n");
                File.WriteAllText(Path.Combine(fixture, "installer.py"), "VERSION = '0.9.17'\n");
                File.WriteAllText(Path.Combine(fixture, "release-config.json"), "{\"version\":\"0.9.17\"}");
                File.WriteAllText(Path.Combine(fixture, ".env"), "NAO_EMP ACOTAR=exemplo");
                File.WriteAllText(Path.Combine(fixture, "Chave-De-Atualizacoes.pem"), "NAO EMPACOTAR");
                File.WriteAllText(Path.Combine(fixture, "accounts.json"), "{\"private\":true}");
                Directory.CreateDirectory(Path.Combine(fixture, "saves")); File.WriteAllText(Path.Combine(fixture, "saves/mundo.txt"), "DADOS DO USUARIO");
                var info = Packager.Inspect(fixture); Packager.Require(info.Version == "0.9.17", "Versão detectada incorreta.");
                Packager.Require(!info.Files.Any(p => p.EndsWith("accounts.json") || p.EndsWith(".env") || p.EndsWith(".pem") || p.Contains("saves")), "Dados privados entraram no payload.");
                results.Add("Seleção dos arquivos do programa, sem incluir contas, mundos ou chaves");
                using (var form = new BuilderForm()) {
                    var drop = new DataObject(DataFormats.FileDrop, new[] { fixture });
                    Packager.Require(form.ReceiveFolderDrop(drop), "A pasta arrastada foi rejeitada.");
                    Packager.Require(form.SelectedFolder == Path.GetFullPath(fixture) && form.DetectedVersion == "0.9.17", "Arrastar a pasta não atualizou a entrada e a versão.");
                    form.Show(); Application.DoEvents();
                    using (var bitmap = new Bitmap(form.Width, form.Height)) { form.DrawToBitmap(bitmap, new Rectangle(Point.Empty, form.Size)); bitmap.Save(Path.Combine(root, "PREVIA-PASTA-ARRASTADA.png")); }
                    form.Close();
                }
                results.Add("Pasta arrastada preenche o campo e detecta a versão na janela");
                ExpectFailure(() => FolderDrop.Read(new DataObject(DataFormats.FileDrop, new[] { Path.Combine(fixture, "KushClient.exe") })), "arquivo arrastado");
                ExpectFailure(() => FolderDrop.Read(new DataObject(DataFormats.FileDrop, new[] { fixture, root })), "várias pastas arrastadas");
                ExpectFailure(() => FolderDrop.Read(new DataObject()), "arraste sem pasta");
                results.Add("Arrastar rejeita arquivos, várias pastas e dados sem pasta");
                File.WriteAllText(Path.Combine(fixture, "core.py"), "VERSION = '0.9.18'\n"); ExpectFailure(() => Packager.Inspect(fixture), "versões diferentes");
                File.WriteAllText(Path.Combine(fixture, "core.py"), "VERSION = '0.9.17'\n"); results.Add("Rejeição de versões divergentes");
                File.WriteAllText(Path.Combine(fixture, "release-config.json"), "{\"version\":\"00.9.17\"}"); ExpectFailure(() => Packager.Inspect(fixture), "versão inválida");
                File.WriteAllText(Path.Combine(fixture, "release-config.json"), "{\"version\":\"0.9.17\"}"); results.Add("Rejeição de versão inválida");
                File.Move(Path.Combine(fixture, "runtime/pythonw.exe"), Path.Combine(fixture, "runtime/guardado.exe")); ExpectFailure(() => Packager.Inspect(fixture), "runtime ausente");
                File.Move(Path.Combine(fixture, "runtime/guardado.exe"), Path.Combine(fixture, "runtime/pythonw.exe")); results.Add("Rejeição de pasta incompleta");
                var before = Directory.GetFiles(fixture, "*", SearchOption.AllDirectories).ToDictionary(p => p, Packager.Hash);
                using (var cancelled = new CancellationTokenSource()) {
                    cancelled.Cancel(); bool rejected = false;
                    try { Packager.Build(fixture, Path.Combine(root, "saida"), s => {}, cancelled.Token); } catch (OperationCanceledException) { rejected = true; }
                    Packager.Require(rejected && !File.Exists(Path.Combine(root, "saida/kushclient-v.0.9.17.exe")), "Cancelamento deixou um EXE publicado.");
                } results.Add("Cancelamento sem saída parcial");
                var output = Packager.Build(fixture, Path.Combine(root, "saida"), s => {}, CancellationToken.None);
                Packager.Require(Packager.PanelVersion(output) == "0.9.17", "Metadados incompatíveis com o painel.");
                Packager.Require(File.ReadAllText(output + ".sha256.txt").StartsWith(Packager.Hash(output)), "SHA-256 incorreto.");
                results.Add("Compilação NSIS, versão interna e SHA-256");
                string original = Packager.Hash(output); ExpectFailure(() => Packager.Build(fixture, Path.Combine(root, "saida"), s => {}, CancellationToken.None), "EXE existente");
                Packager.Require(Packager.Hash(output) == original, "O EXE anterior foi alterado."); results.Add("Preservação de instalador já existente");
                foreach (var kv in before) Packager.Require(File.Exists(kv.Key) && Packager.Hash(kv.Key) == kv.Value, "A pasta original mudou: " + kv.Key);
                results.Add("Pasta original intacta, incluindo nomes com acentos e cifrão");
                File.WriteAllText(Path.Combine(root, "VALIDACAO.json"), Packager.Json().Serialize(new { success = true, passed = results.Count, checks = results }));
                return 0;
            } catch (Exception error) {
                Directory.CreateDirectory(root); File.WriteAllText(Path.Combine(root, "ERRO.txt"), error.ToString()); return 1;
            }
        }
    }
}
