const { kv } = require("@vercel/kv");

const KEY = "farm-ledger:checked";

module.exports = async (req, res) => {
  if (req.method === "GET") {
    const checked = (await kv.get(KEY)) || [];
    res.status(200).json({ checked });
    return;
  }

  if (req.method === "POST") {
    let body = req.body;
    if (typeof body === "string") {
      try {
        body = JSON.parse(body);
      } catch (e) {
        body = {};
      }
    }
    const checked = Array.isArray(body && body.checked)
      ? body.checked.filter(id => typeof id === "string")
      : [];
    await kv.set(KEY, checked);
    res.status(200).json({ checked });
    return;
  }

  res.setHeader("Allow", "GET, POST");
  res.status(405).end("Method Not Allowed");
};
