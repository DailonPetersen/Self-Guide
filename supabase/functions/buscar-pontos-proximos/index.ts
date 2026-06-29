import "jsr:@supabase/functions-js/edge-runtime.d.ts";

Deno.serve(async (req: Request) => {
  const SUPABASE_URL = Deno.env.get('SUPABASE_URL');
  const SERVICE_ROLE_KEY = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY');
  
  if (!SUPABASE_URL || !SERVICE_ROLE_KEY) {
    return new Response(
      JSON.stringify({ error: 'Missing environment variables' }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    );
  }
  
  const { usuario_id, latitude, longitude, idioma } = await req.json();
  
  const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY);
  
  const { data, error } = await supabase.rpc('buscar_pontos_proximos', {
    p_usuario_id: usuario_id,
    p_latitude: latitude,
    p_longitude: longitude,
    p_idioma: idioma ?? 'pt-BR'
  });
  
  if (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    );
  }
  
  return new Response(JSON.stringify(data), {
    headers: { 'Content-Type': 'application/json' },
  });
});