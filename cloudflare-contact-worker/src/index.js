const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
  "x-content-type-options": "nosniff",
};

const ALLOWED_ORIGINS = new Set([
  "https://mazsolaklub.com",
  "https://www.mazsolaklub.com",
]);

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/i;

function json(body, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...JSON_HEADERS, ...extraHeaders },
  });
}

function clean(value, max) {
  if (typeof value !== "string") return "";
  return value.replace(/\u0000/g, "").trim().slice(0, max);
}

function escapeHtml(value) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function countLinks(value) {
  return (value.match(/https?:\/\/|www\./gi) || []).length;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname !== "/api/contact" && url.pathname !== "/api/contact/") {
      return json({ error: "Not found." }, 404);
    }

    if (request.method === "OPTIONS") {
      const origin = request.headers.get("Origin");
      if (!origin || !ALLOWED_ORIGINS.has(origin)) {
        return json({ error: "Forbidden." }, 403);
      }
      return new Response(null, {
        status: 204,
        headers: {
          "access-control-allow-origin": origin,
          "access-control-allow-methods": "POST, OPTIONS",
          "access-control-allow-headers": "content-type",
          "access-control-max-age": "86400",
        },
      });
    }

    if (request.method !== "POST") {
      return json({ error: "Method not allowed." }, 405, { allow: "POST, OPTIONS" });
    }

    const origin = request.headers.get("Origin");
    if (origin && !ALLOWED_ORIGINS.has(origin)) {
      return json({ error: "Forbidden." }, 403);
    }

    const contentType = request.headers.get("content-type") || "";
    if (!contentType.toLowerCase().includes("application/json")) {
      return json({ error: "JSON request required." }, 415);
    }

    const length = Number(request.headers.get("content-length") || 0);
    if (length > 12000) {
      return json({ error: "Request too large." }, 413);
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: "Érvénytelen kérés." }, 400);
    }

    // Honeypot: a valódi felhasználó ezt nem tölti ki.
    if (clean(body.website, 200)) {
      return json({ ok: true });
    }

    const startedAt = Number(body.startedAt || 0);
    if (startedAt && Date.now() - startedAt < 1800) {
      return json({ error: "Túl gyors beküldés. Kérlek próbáld újra." }, 400);
    }

    const name = clean(body.name, 80);
    const email = clean(body.email, 120).toLowerCase();
    const institution = clean(body.institution, 120);
    const message = clean(body.message, 2000);

    if (!email || !EMAIL_RE.test(email)) {
      return json({ error: "Adj meg egy érvényes e-mail címet." }, 400);
    }

    if (message.length < 5) {
      return json({ error: "Írj legalább néhány szót az üzenetbe." }, 400);
    }

    if (countLinks(message) > 3) {
      return json({ error: "Az üzenet túl sok hivatkozást tartalmaz." }, 400);
    }

    const to = env.CONTACT_TO || "klubmazsola@gmail.com";
    const from = env.CONTACT_FROM || "web@mazsolaklub.com";

    const safeName = escapeHtml(name || "Nincs megadva");
    const safeEmail = escapeHtml(email);
    const safeInstitution = escapeHtml(institution || "Nincs megadva");
    const safeMessage = escapeHtml(message).replace(/\n/g, "<br>");

    const subject = institution
      ? "Mazsola Klub dalötlet – " + institution.slice(0, 60)
      : "Mazsola Klub dalötlet / weboldal üzenet";

    const html = `<!doctype html>
<html lang="hu">
<body style="margin:0;padding:0;background:#f4f8fb;font-family:Arial,sans-serif;color:#123656">
  <div style="max-width:640px;margin:0 auto;padding:28px 18px">
    <div style="background:#ffffff;border:1px solid #d7e8f3;border-radius:18px;overflow:hidden">
      <div style="background:#123656;color:#ffffff;padding:22px 26px">
        <div style="font-size:12px;font-weight:700;letter-spacing:1.5px;color:#ffcc35">MAZSOLA KLUB</div>
        <h1 style="font-size:24px;line-height:1.2;margin:7px 0 0">Új dalötlet / üzenet érkezett</h1>
      </div>
      <div style="padding:26px">
        <p style="margin:0 0 18px"><strong>Név:</strong> ${safeName}<br>
        <strong>E-mail:</strong> <a href="mailto:${safeEmail}" style="color:#0866a9">${safeEmail}</a><br>
        <strong>Intézmény:</strong> ${safeInstitution}</p>
        <div style="border-top:1px solid #e2edf4;padding-top:18px">
          <div style="font-size:12px;font-weight:700;letter-spacing:1px;color:#698090;margin-bottom:8px">ÜZENET</div>
          <div style="font-size:16px;line-height:1.65">${safeMessage}</div>
        </div>
      </div>
    </div>
  </div>
</body>
</html>`;

    const text = [
      "Mazsola Klub – új dalötlet / weboldal üzenet",
      "",
      "Név: " + (name || "Nincs megadva"),
      "E-mail: " + email,
      "Intézmény: " + (institution || "Nincs megadva"),
      "",
      "Üzenet:",
      message,
    ].join("\n");

    try {
      await env.EMAIL.send({
        to,
        from: { email: from, name: "Mazsola Klub weboldal" },
        replyTo: { email, name: name || email },
        subject,
        html,
        text,
      });

      return json({ ok: true });
    } catch (error) {
      console.error("Contact form email failed", error?.code, error?.message);
      return json(
        { error: "Az üzenet most nem küldhető el. Kérlek próbáld újra később." },
        500,
      );
    }
  },
};