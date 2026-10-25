from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.units import cm
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
from reportlab.graphics.shapes import Drawing, Rect, String, Line, Polygon

NAME = "Nihit Reddy Syamala"
ROLL = "IMT2023584"
GH = "https://github.com/Nihit3003/calculator   (replace if different)"
DH = "https://hub.docker.com/r/nihit3003/calculator-devops   (replace if different)"

ss = getSampleStyleSheet()
body = ParagraphStyle("b", parent=ss["Normal"], fontSize=9.5, leading=12.5, spaceAfter=4)
h1 = ParagraphStyle("h1", parent=ss["Heading1"], fontSize=14, spaceAfter=6)
h2 = ParagraphStyle("h2", parent=ss["Heading2"], fontSize=11.5, spaceBefore=8, spaceAfter=4)
cell = ParagraphStyle("c", parent=body, fontSize=8.8, leading=11, spaceAfter=0)
note = ParagraphStyle("n", parent=body, backColor=colors.HexColor("#FFF4CC"), borderPadding=5, textColor=colors.HexColor("#5a4500"))

def arrow(d, x1, y, x2):
    d.add(Line(x1, y, x2 - 4, y, strokeColor=colors.HexColor("#444444"), strokeWidth=1.2))
    d.add(Polygon([x2, y, x2 - 6, y + 3, x2 - 6, y - 3], fillColor=colors.HexColor("#444444"), strokeColor=colors.HexColor("#444444")))

def box(d, x, y, w, h, text, fill):
    d.add(Rect(x, y, w, h, rx=4, ry=4, fillColor=colors.HexColor(fill), strokeColor=colors.HexColor("#333333")))
    lines = text.split("\n")
    top = y + h / 2 + (len(lines) - 1) * 5 - 3
    for i, t in enumerate(lines):
        d.add(String(x + w / 2, top - i * 10, t, textAnchor="middle", fontSize=7.8, fontName="Helvetica-Bold" if i == 0 else "Helvetica"))

def diagram():
    d = Drawing(17 * cm, 6.4 * cm)
    # Row 1: flow
    row1 = [("Developer\ngit push", "#E3F2FD"), ("GitHub\nrepository", "#E3F2FD"), ("Jenkins\nwebhook / poll", "#FFE0B2"),
            ("Maven\nbuild + JUnit", "#FFE0B2"), ("Docker\nbuild image", "#C8E6C9")]
    w, h, gap, x0, y = 2.8 * cm, 1.3 * cm, 0.75 * cm, 0.1 * cm, 4.6 * cm
    xs = []
    for i, (t, c) in enumerate(row1):
        x = x0 + i * (w + gap); xs.append(x)
        box(d, x, y, w, h, t, c)
        if i: arrow(d, x - gap, y + h / 2, x)
    # Row 2: push + deploy
    box(d, xs[4], 2.4 * cm, w, h, "Docker Hub\nnihit3003/\ncalculator-devops", "#C8E6C9")
    d.add(Line(xs[4] + w / 2, y, xs[4] + w / 2, 2.4 * cm + h + 4, strokeColor=colors.HexColor("#444444"), strokeWidth=1.2))
    d.add(Polygon([xs[4] + w / 2, 2.4 * cm + h, xs[4] + w / 2 - 3, 2.4 * cm + h + 6, xs[4] + w / 2 + 3, 2.4 * cm + h + 6], fillColor=colors.HexColor("#444444")))
    box(d, xs[2], 2.4 * cm, w, h, "Jenkins Deploy\ndocker compose\nup -d", "#FFE0B2")
    yy = 2.4 * cm + h / 2
    d.add(Line(xs[4], yy, xs[2] + w + 4, yy, strokeColor=colors.HexColor("#444444"), strokeWidth=1.2))
    d.add(Polygon([xs[2] + w, yy, xs[2] + w + 6, yy + 3, xs[2] + w + 6, yy - 3], fillColor=colors.HexColor("#444444")))
    # fix arrow direction: draw pointing left
    # Deploy -> stack
    box(d, xs[0], 0.2 * cm, 3 * w + 2 * gap, 1.3 * cm, "Compose stack (network: calculator-net)\ncalculator-proxy (Nginx, host :8082)  ->  calculator-app (Java, host :8081)", "#F3E5F5")
    d.add(Line(xs[2] + w / 2, 2.4 * cm, xs[2] + w / 2, 1.5 * cm + 3, strokeColor=colors.HexColor("#444444"), strokeWidth=1.2))
    d.add(Polygon([xs[2] + w / 2, 1.5 * cm, xs[2] + w / 2 - 3, 1.5 * cm + 6, xs[2] + w / 2 + 3, 1.5 * cm + 6], fillColor=colors.HexColor("#444444")))
    # Email
    box(d, xs[0], 2.4 * cm, w, h, "Email\nnotification", "#FFCDD2")
    d.add(Line(xs[0] + w, 2.4 * cm + h / 2, xs[2], 2.4 * cm + h / 2, strokeColor=colors.HexColor("#999999"), strokeDashArray=[3, 2]))
    d.add(String(xs[0] + w + 0.1 * cm, 2.4 * cm + h / 2 + 3, "success / failure (post)", fontSize=6.5, fillColor=colors.HexColor("#666666")))
    return d

def table(data, widths):
    t = Table([[Paragraph(str(c), cell) for c in r] for r in data], colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#DDE6F0")),
                           ("GRID", (0, 0), (-1, -1), 0.4, colors.grey), ("VALIGN", (0, 0), (-1, -1), "TOP"),
                           ("TOPPADDING", (0, 0), (-1, -1), 2), ("BOTTOMPADDING", (0, 0), (-1, -1), 2)]))
    return t

doc = SimpleDocTemplate("IMT2023584_Nihit_Reddy_Syamala.pdf", pagesize=A4, leftMargin=2*cm, rightMargin=2*cm,
                        topMargin=1.7*cm, bottomMargin=1.6*cm, title="CSE 816 Mini-Project Report", author=NAME)
s = []
s.append(Paragraph("End-to-End CI/CD Pipeline for a Simple Java Calculator", h1))
s.append(Paragraph(f"<b>{NAME}</b> &nbsp;|&nbsp; {ROLL} &nbsp;|&nbsp; CSE 816 Software Production Engineering, M.Tech (CSE) Term I, 2026-27 &nbsp;|&nbsp; Prof. B. Thangaraju", body))
s.append(Paragraph(f"<b>GitHub:</b> {GH}<br/><b>Docker Hub:</b> {DH}", body))

s.append(Paragraph("1. Project summary", h2))
s.append(Paragraph("A small calculator (add, subtract, multiply, divide) is written in plain Java with Maven and JUnit 5. Following the instructor's direction, "
                   "it is a simple Java application and does not use Spring Boot. It has a command-line mode and a small server mode built on the JDK's "
                   "own HTTP server, which provides a real <font face='Courier'>/health</font> endpoint (not Spring Actuator) so Docker Compose can run health checks "
                   "and Nginx has something to proxy. A push to GitHub triggers Jenkins, which builds, tests, packages a Docker image, pushes it to Docker Hub and deploys it with Docker Compose.", body))

s.append(Paragraph("2. CI/CD pipeline architecture", h2))
s.append(diagram())
s.append(Paragraph("Scope is limited to Docker Compose deployment (no Ansible, Kubernetes or Prometheus/Grafana).", body))

s.append(Paragraph("3. Pipeline stages", h2))
s.append(table([
    ["Stage", "What it does", "Failure behaviour"],
    ["Checkout", "<font face='Courier'>checkout scm</font> fetches the latest commit from GitHub, triggered by webhook or Poll SCM.", "Pipeline stops."],
    ["Build", "<font face='Courier'>mvn -B clean package -DskipTests</font> compiles and creates <font face='Courier'>target/calculator.jar</font>.", "Pipeline stops."],
    ["Test", "<font face='Courier'>mvn -B test</font> runs JUnit 5 tests; XML reports are published with the <font face='Courier'>junit</font> step.", "Failing tests stop the pipeline, so nothing is pushed or deployed."],
    ["Docker Build", "Multi-stage Dockerfile (Maven+JDK build, JRE-alpine runtime, non-root, HEALTHCHECK). Tagged with the build number and <font face='Courier'>latest</font>.", "Pipeline stops."],
    ["Push", "<font face='Courier'>docker login --password-stdin</font> using the Jenkins credential <font face='Courier'>dockerhub-credentials</font>, then pushes both tags.", "Pipeline stops; no secret is stored in Git."],
    ["Deploy", "<font face='Courier'>docker compose up -d</font> with <font face='Courier'>IMAGE_TAG</font> = build number, waits for both containers to be healthy, then calls the API through Nginx.", "Logs are printed and the build fails."],
    ["post", "Success and failure emails to the student; <font face='Courier'>docker image prune</font> cleanup.", "-"],
], [2.2*cm, 10.3*cm, 4.5*cm]))
s.append(Spacer(1, 4))
s.append(Paragraph("<b>Key design points.</b> Testing: 41 JUnit tests cover each operation, divide by zero, missing/invalid input, CLI exit codes and HTTP status codes/JSON. "
                   "Compose: the app and an Nginx proxy share the user-defined network <font face='Courier'>calculator-net</font>, use restart policies and health checks, and Nginx waits for the app "
                   "(<font face='Courier'>service_healthy</font>), while its own check hits <font face='Courier'>/nginx-health</font>, which avoids circular dependencies. "
                   "Idempotence: fixed project and container names plus a unique image tag per build mean re-running the job safely updates the same stack.", body))

s.append(Paragraph("4. Challenges faced", h2))
s.append(Paragraph("[TO BE COMPLETED BY STUDENT: describe the real problems you met while setting this up (for example Jenkins access to Docker, webhook reachability for a local Jenkins, "
                   "SMTP/App Password setup, port conflicts), how you diagnosed them and how you fixed them. Delete this note before submitting.]", note))
s.append(Paragraph("Design challenges that were resolved while preparing the project:", body))
for t in ["A plain console program exits immediately, so it cannot be health-checked or proxied. Solved with an optional server mode on the JDK HTTP server.",
          "Jenkins uses port 8080 by default, so the app is published on 8081 and the proxy on 8082.",
          "A local Jenkins cannot receive GitHub webhooks, so the Jenkinsfile also includes Poll SCM as a fallback."]:
    s.append(Paragraph("&bull; " + t, body))

s.append(Paragraph("5. Evidence (to be attached by student)", h2))
s.append(Paragraph("[Insert real screenshots here: green Jenkins run with stage view and test results, email notification, <font face='Courier'>docker ps</font>, sample API responses, Docker Hub page.]", note))
doc.build(s)
