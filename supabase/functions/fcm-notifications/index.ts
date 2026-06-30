import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

serve(async (req: Request) => {
  const body = await req.json()
  const { titulo, mensagem, usuarioIds, pontoId } = body

  // Send push notification via FCM
  // Implementation would use Firebase Admin SDK

  return new Response(
    JSON.stringify({ success: true }),
    { status: 200, headers: { "Content-Type": "application/json" } }
  )
})