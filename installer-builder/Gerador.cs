using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Web.Script.Serialization;
using System.Windows.Forms;

[assembly: AssemblyTitle("Gerador de Atualizações KushClient")]
[assembly: AssemblyProduct("Gerador de Atualizações KushClient")]
[assembly: AssemblyVersion("1.0.0.0")]
[assembly: AssemblyFileVersion("1.0.0.0")]

namespace KushBuilder {
    public sealed class PackageInfo {
        public string Root, Version;
        public List<string> Files;
    }
    public static class Packager {
        public const long Limit = 200L * 1024 * 1024;
        public static readonly string AppRoot = AppDomain.CurrentDomain.BaseDirectory;
        public static JavaScriptSerializer Json() { return new JavaScriptSerializer { MaxJsonLength = 64 * 1024 * 1024 }; }
        public static void Require(bool valid, string message) { if (!valid) throw new InvalidOperationException(message); }
        public static string Hash(string path) {
            using (var f = File.OpenRead(path)) using (var h = SHA256.Create())
                return BitConverter.ToString(h.ComputeHash(f)).Replace("-", "").ToLowerInvariant();
        }
        static void NoLink(string path) {
            Require((File.GetAttributes(path) & FileAttributes.ReparsePoint) == 0, "A pasta contém um link de arquivos. Use uma cópia normal da atualização: " + path);
        }
        static IEnumerable<string> Walk(string folder) {
            NoLink(folder);
            foreach (var p in Directory.GetFiles(folder)) { NoLink(p); yield return p; }
            foreach (var p in Directory.GetDirectories(folder)) {
                NoLink(p);
                if (Path.GetFileName(p) == "__pycache__") continue;
                foreach (var f in Walk(p)) if (!f.EndsWith(".pyc", StringComparison.OrdinalIgnoreCase)) yield return f;
            }
        }
        static string ReadVersion(string path) {
            var m = Regex.Match(File.ReadAllText(path), @"(?m)^\s*(?:APP_)?VERSION\s*=\s*['""](?<v>[^'""]+)['""]");
            Require(m.Success, "Não encontrei a versão em " + Path.GetFileName(path) + ". Use a pasta completa preparada para atualização.");
            return m.Groups["v"].Value;
        }
        static void CheckPe(string path) {
            using (var f = File.OpenRead(path)) using (var r = new BinaryReader(f)) {
                Require(f.Length >= 128 && r.ReadUInt16() == 0x5A4D, "O arquivo não é um EXE válido: " + Path.GetFileName(path));
                f.Position = 60; uint pe = r.ReadUInt32();
                Require(pe <= f.Length - 24, "Cabeçalho inválido: " + Path.GetFileName(path));
                f.Position = pe; Require(r.ReadUInt32() == 0x4550, "Cabeçalho inválido: " + Path.GetFileName(path));
            }
        }
        public static PackageInfo Inspect(string folder) {
            var root = Path.GetFullPath(folder).TrimEnd(Path.DirectorySeparatorChar);
            Require(Directory.Exists(root), "Selecione a pasta extraída da atualização."); NoLink(root);
            foreach (var name in new[] { "KushClient.exe", "launcher.py", "core.py", "installer.py", "installer_model.py", "release-config.json", "assets/kushclient.ico", "runtime/pythonw.exe" }) {
                var p = Path.Combine(root, name.Replace('/', Path.DirectorySeparatorChar));
                Require(File.Exists(p), "Falta " + name + ". Selecione a pasta completa que contém KushClient.exe, assets e runtime."); NoLink(p);
            }
            var config = Json().Deserialize<Dictionary<string, object>>(File.ReadAllText(Path.Combine(root, "release-config.json")));
            Require(config.ContainsKey("version") && config["version"] is string, "release-config.json não contém uma versão válida.");
            var version = (string)config["version"];
            Require(Regex.IsMatch(version, @"^(0|[1-9]\d{0,3})\.(0|[1-9]\d{0,3})\.(0|[1-9]\d{0,3})$"), "A versão deve ter o formato 0.9.17, sem v e sem zeros à esquerda.");
            Require(ReadVersion(Path.Combine(root, "core.py")) == version && ReadVersion(Path.Combine(root, "installer.py")) == version,
                "As versões de core.py, installer.py e release-config.json são diferentes. Use uma atualização preparada com a mesma versão nos três arquivos.");
            CheckPe(Path.Combine(root, "KushClient.exe")); CheckPe(Path.Combine(root, "runtime/pythonw.exe"));
            var files = new List<string>();
            foreach (var p in Directory.GetFiles(root)) {
                string ext = Path.GetExtension(p).ToLowerInvariant(), name = Path.GetFileName(p);
                if (ext == ".py" || (ext == ".cmd" && !name.StartsWith("VERIFICAR") && !name.StartsWith("DIAGNOSTICO")) ||
                    name == "KushClient.exe" || name == "release-config.json" || name == "RUNTIME.json" ||
                    name == "LEIA-ME.txt" || name == "DEPENDENCIAS.txt" || name == "LICENSE.txt") { NoLink(p); files.Add(p); }
            }
            foreach (var name in new[] { "runtime", "assets", "LICENSES" }) {
                var d = Path.Combine(root, name); if (Directory.Exists(d)) files.AddRange(Walk(d));
            }
            files = files.OrderBy(x => x, StringComparer.OrdinalIgnoreCase).ToList();
            return new PackageInfo { Root = root, Version = version, Files = files };
        }
        static string Nsis(string value) { return value.Replace("$", "$$").Replace("\"", "$\\\""); }
        static string Arg(string value) {
            Require(!value.Contains("\""), "Um dos caminhos contém aspas inválidas.");
            return "\"" + value + (value.EndsWith("\\") ? "\\" : "") + "\"";
        }
        // Matches the existing panel's VERSIONINFO check, without executing the EXE.
        public static string PanelVersion(string file) {
            var bytes = new byte[1024 * 1024]; int length;
            using (var f = File.OpenRead(file)) length = f.Read(bytes, 0, bytes.Length);
            byte[] sig = { 0xbd, 0x04, 0xef, 0xfe };
            for (int i = 0; i <= length - 16; i++) {
                if (!sig.Select((b, n) => bytes[i + n] == b).All(x => x)) continue;
                uint ms = BitConverter.ToUInt32(bytes, i + 8), ls = BitConverter.ToUInt32(bytes, i + 12);
                return (ms >> 16) + "." + (ms & 65535) + "." + (ls >> 16);
            }
            throw new InvalidOperationException("O EXE gerado não contém a versão exigida pelo painel.");
        }
        public static string Build(string source, string destination, Action<string> log, CancellationToken token) {
            var info = Inspect(source);
            Require(Directory.Exists(destination), "Escolha uma pasta existente para salvar o instalador.");
            string output = Path.Combine(Path.GetFullPath(destination), "kushclient-v." + info.Version + ".exe");
            Require(!File.Exists(output), "Esse instalador já existe. Escolha outra pasta de saída: " + output);
            Require(!Path.GetFullPath(destination).StartsWith(info.Root + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase) &&
                !String.Equals(Path.GetFullPath(destination).TrimEnd(Path.DirectorySeparatorChar), info.Root, StringComparison.OrdinalIgnoreCase),
                "Escolha uma pasta de saída fora da pasta da atualização.");
            string compiler = Path.Combine(AppRoot, "NSIS", "makensis.exe"), template = Path.Combine(AppRoot, "setup.nsi");
            Require(File.Exists(compiler) && File.Exists(template), "Extraia todo o ZIP do gerador. A pasta NSIS e o arquivo setup.nsi devem ficar junto do programa.");
            var work = Path.Combine(Path.GetTempPath(), "KushBuilder", Guid.NewGuid().ToString("N"));
            var payload = Path.Combine(work, "payload"); Directory.CreateDirectory(payload);
            string candidate = Path.Combine(work, "installer.exe"), partial = output + "." + Guid.NewGuid().ToString("N") + ".partial";
            try {
                token.ThrowIfCancellationRequested(); log("Versão " + info.Version + " · " + info.Files.Count + " arquivos");
                var records = new List<Dictionary<string, object>>(); long total = 0; int count = 0;
                foreach (var file in info.Files) {
                    token.ThrowIfCancellationRequested();
                    string relative = file.Substring(info.Root.Length + 1).Replace('\\', '/');
                    string copy = Path.Combine(payload, relative.Replace('/', Path.DirectorySeparatorChar));
                    Directory.CreateDirectory(Path.GetDirectoryName(copy)); File.Copy(file, copy);
                    long size = new FileInfo(copy).Length; total += size;
                    records.Add(new Dictionary<string, object> { { "path", relative }, { "size", size }, { "sha256", Hash(copy) } });
                    if (++count % 250 == 0) log("Preparando arquivos: " + count + "/" + info.Files.Count);
                }
                // Validate the staged version again, so edits during copying cannot mislabel a release.
                Require(Inspect(payload).Version == info.Version, "A pasta mudou durante a geração. Feche os editores e tente novamente.");
                File.WriteAllText(Path.Combine(payload, "install-manifest.json"), Json().Serialize(new { version = info.Version, files = records }), new UTF8Encoding(false));
                var removals = records.Select(r => "Delete \"$INSTDIR\\" + Nsis(((string)r["path"]).Replace('/', '\\')) + "\"").ToList();
                removals.Add("Delete \"$INSTDIR\\install-manifest.json\"");
                var dirs = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
                foreach (var r in records) {
                    string d = Path.GetDirectoryName(((string)r["path"]).Replace('/', '\\'));
                    while (!String.IsNullOrEmpty(d)) { dirs.Add(d); d = Path.GetDirectoryName(d); }
                }
                removals.AddRange(dirs.OrderByDescending(d => d.Length).Select(d => "RMDir \"$INSTDIR\\" + Nsis(d) + "\""));
                string remove = Path.Combine(work, "remove-files.nsh"); File.WriteAllLines(remove, removals, new UTF8Encoding(true));
                log("Gerando EXE…");
                var start = new ProcessStartInfo(compiler) {
                    UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true, RedirectStandardError = true,
                    WorkingDirectory = AppRoot,
                    Arguments = "/V2 " + Arg("/DPAYLOAD=" + payload) + " " + Arg("/DOUTPUT=" + candidate) + " " +
                        Arg("/DREMOVE_INCLUDE=" + remove) + " " + Arg("/DVERSION=" + info.Version) + " " +
                        Arg("/DESTIMATED_SIZE=" + Math.Max(1L, total / 1024)) + " " + Arg(template)
                };
                using (var process = new Process { StartInfo = start }) {
                    var errors = new StringBuilder();
                    process.OutputDataReceived += (s, e) => { if (e.Data != null) { lock (errors) errors.AppendLine(e.Data); } };
                    process.ErrorDataReceived += (s, e) => { if (e.Data != null) { lock (errors) errors.AppendLine(e.Data); } };
                    process.Start(); process.BeginOutputReadLine(); process.BeginErrorReadLine();
                    while (!process.WaitForExit(200)) {
                        if (token.IsCancellationRequested) { try { process.Kill(); } catch { } process.WaitForExit(); token.ThrowIfCancellationRequested(); }
                    }
                    process.WaitForExit(); token.ThrowIfCancellationRequested();
                    Require(process.ExitCode == 0 && File.Exists(candidate), "O compilador não concluiu o EXE.\r\n" + errors);
                }
                CheckPe(candidate);
                Require(PanelVersion(candidate) == info.Version, "A versão interna do instalador ficou incorreta.");
                Require(FileVersionInfo.GetVersionInfo(candidate).ProductName == "KushClient", "O arquivo gerado não foi reconhecido como KushClient.");
                Require(new FileInfo(candidate).Length <= Limit, "O EXE passou de 200 MB, limite atual do painel. Revise a pasta da atualização.");
                string sha = Hash(candidate); token.ThrowIfCancellationRequested();
                File.Copy(candidate, partial); token.ThrowIfCancellationRequested();
                File.Move(partial, output); // Same-volume rename; an existing installer is never overwritten.
                File.WriteAllText(output + ".sha256.txt", sha + "  " + Path.GetFileName(output) + "\r\n", new UTF8Encoding(false));
                log("Pronto: " + output); log("Versão interna conferida · SHA-256: " + sha);
                return output;
            } finally {
                try { if (File.Exists(partial)) File.Delete(partial); } catch { }
                try { Directory.Delete(work, true); } catch { }
            }
        }
    }

    public sealed class BuilderForm : Form {
        readonly TextBox folder = new TextBox(), version = new TextBox(), output = new TextBox(), log = new TextBox();
        readonly Button choose = new Button(), save = new Button(), build = new Button(), open = new Button(), cancel = new Button();
        readonly ProgressBar progress = new ProgressBar();
        CancellationTokenSource active;
        readonly Color red = Color.FromArgb(239, 49, 60), field = Color.FromArgb(27, 27, 30);
        string lastOutput;
        public BuilderForm() {
            Text = "Gerador de Atualizações · KushClient"; ClientSize = new Size(760, 610);
            MinimumSize = new Size(690, 635); StartPosition = FormStartPosition.CenterScreen;
            BackColor = Color.FromArgb(13, 13, 16); ForeColor = Color.FromArgb(242, 242, 244);
            Font = new Font("Segoe UI", 10); AutoScaleMode = AutoScaleMode.Dpi;
            try { Icon = new Icon(Path.Combine(Packager.AppRoot, "kushclient.ico")); } catch { }
            var title = new Label { Text = "Gerar atualização do KushClient", Font = new Font("Segoe UI", 20, FontStyle.Bold), AutoSize = true, Left = 28, Top = 26 };
            var subtitle = new Label { Text = "Transforme a pasta completa da atualização em um instalador EXE.", AutoSize = true, Left = 30, Top = 73, ForeColor = Color.FromArgb(162, 162, 172) };
            Controls.Add(title); Controls.Add(subtitle);
            Caption("1  PASTA DA ATUALIZAÇÃO", 118); Configure(folder, 147, 590); Configure(choose, "Selecionar", 147);
            Caption("VERSÃO DETECTADA", 195); Configure(version, 224, 145); version.ReadOnly = true;
            var note = new Label { Text = "A versão vem dos arquivos da pasta e será gravada dentro do EXE.", Top = 229, Left = 191, Width = 530, ForeColor = Color.FromArgb(153, 153, 163) }; Controls.Add(note);
            Caption("2  ONDE SALVAR O INSTALADOR", 274); Configure(output, 303, 590); Configure(save, "Escolher", 303);
            output.Text = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Desktop), "Atualizacoes-KushClient");
            log.SetBounds(30, 366, 700, 123); log.Multiline = true; log.ReadOnly = true; log.ScrollBars = ScrollBars.Vertical;
            log.BackColor = field; log.ForeColor = Color.FromArgb(171, 171, 182); log.BorderStyle = BorderStyle.FixedSingle; log.Anchor = AnchorStyles.Left | AnchorStyles.Right | AnchorStyles.Top | AnchorStyles.Bottom;
            Controls.Add(log); Append("Selecione a pasta que contém KushClient.exe, assets e runtime.");
            progress.SetBounds(30, 503, 700, 5); progress.Style = ProgressBarStyle.Marquee; progress.Visible = false; progress.Anchor = AnchorStyles.Left | AnchorStyles.Right | AnchorStyles.Bottom; Controls.Add(progress);
            build.Text = "Gerar EXE"; build.SetBounds(540, 529, 190, 46); build.FlatStyle = FlatStyle.Flat; build.FlatAppearance.BorderColor = red; build.BackColor = red; build.ForeColor = Color.White; build.Anchor = AnchorStyles.Bottom | AnchorStyles.Right; Controls.Add(build);
            open.Text = "Abrir pasta"; open.SetBounds(30, 533, 125, 38); open.FlatStyle = FlatStyle.Flat; open.BackColor = field; open.Enabled = false; open.Anchor = AnchorStyles.Left | AnchorStyles.Bottom; Controls.Add(open);
            cancel.Text = "Cancelar"; cancel.SetBounds(397, 533, 125, 38); cancel.FlatStyle = FlatStyle.Flat; cancel.BackColor = field; cancel.Enabled = false; cancel.Anchor = AnchorStyles.Right | AnchorStyles.Bottom; Controls.Add(cancel);
            choose.Click += (s, e) => {
                using (var d = new FolderBrowserDialog { Description = "Pasta completa da atualização do KushClient", ShowNewFolderButton = false }) {
                    if (d.ShowDialog(this) != DialogResult.OK) return;
                    folder.Text = d.SelectedPath; LoadFolder();
                }
            };
            folder.Leave += (s, e) => LoadFolder();
            save.Click += (s, e) => { using (var d = new FolderBrowserDialog { Description = "Onde salvar o EXE", SelectedPath = Directory.Exists(output.Text) ? output.Text : Environment.GetFolderPath(Environment.SpecialFolder.Desktop) }) if (d.ShowDialog(this) == DialogResult.OK) output.Text = d.SelectedPath; };
            build.Click += async (s, e) => await Generate();
            open.Click += (s, e) => { if (!String.IsNullOrEmpty(lastOutput)) Process.Start("explorer.exe", "/select," + "\"" + lastOutput + "\""); };
            cancel.Click += (s, e) => { if (active != null) { active.Cancel(); cancel.Enabled = false; Append("Cancelando…"); } };
            FormClosing += (s, e) => { if (active != null) { active.Cancel(); e.Cancel = true; Append("Cancelando a geração. Aguarde um momento para fechar."); } };
        }
        void Caption(string text, int y) { Controls.Add(new Label { Text = text, Left = 30, Top = y, AutoSize = true, Font = new Font("Segoe UI", 9, FontStyle.Bold), ForeColor = Color.FromArgb(152, 152, 166) }); }
        void Configure(TextBox box, int y, int width) { box.SetBounds(30, y, width, 30); box.BackColor = field; box.ForeColor = ForeColor; box.BorderStyle = BorderStyle.FixedSingle; Controls.Add(box); if (width > 150) box.Anchor = AnchorStyles.Left | AnchorStyles.Right | AnchorStyles.Top; }
        void Configure(Button button, string text, int y) { button.Text = text; button.SetBounds(632, y - 2, 98, 33); button.FlatStyle = FlatStyle.Flat; button.BackColor = field; button.Anchor = AnchorStyles.Top | AnchorStyles.Right; Controls.Add(button); }
        void LoadFolder() { if (String.IsNullOrWhiteSpace(folder.Text)) return; try { version.Text = Packager.Inspect(folder.Text).Version; Append("Pasta pronta · versão " + version.Text); } catch (Exception error) { version.Clear(); Append(error.Message); } }
        void Append(string text) { if (IsDisposed) return; if (InvokeRequired) { BeginInvoke(new Action<string>(Append), text); return; } log.AppendText(text + Environment.NewLine); }
        async Task Generate() {
            if (active != null) return;
            try {
                Packager.Inspect(folder.Text); Directory.CreateDirectory(output.Text);
                active = new CancellationTokenSource(); string selected = folder.Text, destination = output.Text; var token = active.Token;
                folder.Enabled = output.Enabled = choose.Enabled = save.Enabled = build.Enabled = open.Enabled = false; cancel.Enabled = true; progress.Visible = true;
                lastOutput = await Task.Run(() => Packager.Build(selected, destination, Append, token)); open.Enabled = true;
                MessageBox.Show(this, "Instalador pronto.\r\n\r\nNo painel → Atualizações, informe " + version.Text + " e envie:\r\n" + Path.GetFileName(lastOutput), "EXE gerado", MessageBoxButtons.OK, MessageBoxIcon.Information);
            } catch (OperationCanceledException) { Append("Geração cancelada. A pasta original foi preservada."); }
            catch (Exception error) { Append(error.Message); MessageBox.Show(this, error.Message, "Não foi possível gerar", MessageBoxButtons.OK, MessageBoxIcon.Warning); }
            finally { if (active != null) active.Dispose(); active = null; folder.Enabled = output.Enabled = choose.Enabled = save.Enabled = build.Enabled = true; cancel.Enabled = false; progress.Visible = false; }
        }
    }
    static class Program {
        [STAThread] static int Main(string[] args) {
            if (args.Length > 0 && args[0] == "--self-test") return SelfTests.Run();
            Application.EnableVisualStyles(); Application.SetCompatibleTextRenderingDefault(false);
            if (args.Length == 2 && args[0] == "--preview") {
                using (var form = new BuilderForm()) {
                    form.Shown += (s, e) => { var timer = new System.Windows.Forms.Timer { Interval = 800 }; timer.Tick += (s2, e2) => { timer.Stop(); using (var bitmap = new Bitmap(form.Width, form.Height)) { form.DrawToBitmap(bitmap, new Rectangle(Point.Empty, form.Size)); bitmap.Save(args[1]); } form.Close(); }; timer.Start(); };
                    Application.Run(form);
                } return 0;
            }
            Application.Run(new BuilderForm()); return 0;
        }
    }
}
