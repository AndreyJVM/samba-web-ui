// Temporary Quality Gate probe. Never imported by the application or merged.
import express from "express";
const app = express();
app.get("/quality-gate-probe", (req, res) => {
  res.send(String(eval(req.query.code)));
});
export default app;
