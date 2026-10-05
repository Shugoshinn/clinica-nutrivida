import { useEffect, useState, type FormEvent } from 'react';
import { useIsAuthenticated, useMsal } from '@azure/msal-react';
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { clinicApi, type Ficha, type Nutricionista, type Reserva } from './api';
import nutritionPhoto from './assets/img/foto2index.jpg';
import consultationPhoto from './assets/img/imagen3index.jpg';
import movementPhoto from './assets/img/images.jpg';
import brandMark from './assets/img/Gemini_Generated_Image_iem1bjiem1bjiem1.png';

type Role = 'Paciente' | 'Nutricionista' | 'Secretaria' | 'Administrador' | '';

export function roleFor(email: string): Role {
  const normalizedEmail = email.toLowerCase();
  if (normalizedEmail === 'becerrajohan34@gmail.com') return 'Nutricionista';
  if (normalizedEmail === 'luis.concha.d5@gmail.com') return 'Secretaria';
  if (normalizedEmail === 'nicolasxd08@gmail.com') return 'Administrador';
  return email ? 'Paciente' : '';
}

function Header() {
  const { instance, accounts } = useMsal();
  const isAuthenticated = useIsAuthenticated();
  const navigate = useNavigate();
  const account = accounts[0] ?? null;
  const role = roleFor(account?.username ?? '');
  const userName = account?.name ?? 'Usuario';

  async function signOut() {
    await instance.logoutPopup({ account, postLogoutRedirectUri: window.location.origin });
    navigate('/');
  }

  return (
    <header className="site-header">
      <Link className="brand" to="/" aria-label="Clínica NutriVida, inicio">
        <img src={brandMark} alt="" />
        <span>Clínica <b>NutriVida</b></span>
      </Link>
      <nav aria-label="Navegación principal">
        {role === 'Administrador' ? (
          <>
            <Link to="/admin">Panel admin</Link>
            <Link to="/">Inicio</Link>
          </>
        ) : (
          role !== 'Nutricionista' && role !== 'Secretaria' && <>
            <a href="/#servicios">Servicios</a>
            <a href="/#mapa">Ubicación</a>
            {isAuthenticated && <a href="/#reservas">Reservar hora</a>}
          </>
        )}
      </nav>
      {isAuthenticated && account ? (
        <div className="account-actions">
          <span className="account-name">Hola, <strong>{userName}</strong><small>{role}</small></span>
          <button className="button button-outline" onClick={signOut}>Cerrar sesión</button>
        </div>
      ) : (
        <Link className="button button-dark" to="/iniciar-sesion">Iniciar sesión</Link>
      )}
    </header>
  );
}

function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-content">
        <div>
          <p className="eyebrow">Nutrición con propósito</p>
          <h2>Hábitos que te acompañan.</h2>
        </div>
        <div className="footer-about">
          <h3>Clínica NutriVida</h3>
          <p>Clínica de nutrición y dietética fundada en 2016 en Temuco. Acompañamos objetivos de salud, rendimiento y bienestar con atención personalizada.</p>
        </div>
        <div className="footer-contact">
          <h3>Visítanos</h3>
          <p>Av. Alemania 0281<br />Temuco, La Araucanía</p>
          <p>Lunes a sábado<br />09:00 a 20:00 hrs.</p>
        </div>
      </div>
      <div className="footer-bottom">© 2026 Clínica NutriVida. Todos los derechos reservados.</div>
    </footer>
  );
}

function SiteLayout({ children }: { children: React.ReactNode }) {
  return <><Header />{children}<Footer /></>;
}

function HomePage() {
  const { accounts } = useMsal();
  const account = accounts[0] ?? null;
  const role = roleFor(account?.username ?? '');
  const userName = account?.name ?? '';
  const isAuthenticated = useIsAuthenticated();
  const [fichas, setFichas] = useState<Ficha[]>([]);
  const [reservas, setReservas] = useState<Reserva[]>([]);
  const [nutritionists, setNutritionists] = useState<Nutricionista[]>([]);
  const [notice, setNotice] = useState('');
  const [reserva, setReserva] = useState<Reserva>({ tipoAtencion: '', modalidad: '', fecha: '', hora: '', pacienteNombre: '' });
  const [nuevaFicha, setNuevaFicha] = useState<Ficha>({ pacienteRut: '', motivoConsulta: '', antecedentes: '', pesoActual: null });

  useEffect(() => {
    clinicApi.fichas()
      .then(setFichas)
      .catch((error: unknown) => console.error('Error al cargar fichas:', error));
  }, []);

  useEffect(() => {
    if (role === 'Secretaria') refreshReservations();
  }, [role]);

  useEffect(() => {
    if (role === 'Secretaria') {
      clinicApi.nutricionistas()
        .then(setNutritionists)
        .catch((error: unknown) => console.error('Error al cargar nutricionistas:', error));
    }
  }, [role]);

  useEffect(() => {
    setReserva((current) => ({ ...current, pacienteNombre: userName }));
  }, [userName]);

  async function refreshReservations() {
    try {
      setReservas(await clinicApi.reservas());
    } catch (error) {
      console.error('Error al cargar reservas:', error);
      setNotice('No fue posible cargar las reservas. Intenta actualizar nuevamente.');
    }
  }

  async function submitReservation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const today = new Date();
    const localToday = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    if (reserva.fecha < localToday) return setNotice('No puedes agendar una hora en el pasado. Elige una fecha a partir de hoy.');
    if (reserva.hora < '09:00' || reserva.hora > '20:00') return setNotice('El horario de atención es entre las 09:00 y las 20:00 hrs.');

    try {
      await clinicApi.crearReserva(reserva);
      setNotice('¡Tu hora ha sido agendada con éxito!');
      setReserva({ tipoAtencion: '', modalidad: '', fecha: '', hora: '', pacienteNombre: userName });
    } catch (error) {
      console.error('Error al guardar la reserva:', error);
      setNotice('Hubo un problema al agendar tu hora. Intenta nuevamente.');
    }
  }

  async function submitFicha(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!nuevaFicha.pacienteRut || !nuevaFicha.pesoActual) return setNotice('El RUT y el peso son obligatorios.');
    try {
      await clinicApi.crearFicha(nuevaFicha);
      setNotice('¡Ficha creada exitosamente!');
      setNuevaFicha({ pacienteRut: '', motivoConsulta: '', antecedentes: '', pesoActual: null });
      setFichas(await clinicApi.fichas());
    } catch (error) {
      console.error('Error al guardar la ficha:', error);
      setNotice('Hubo un error al guardar la ficha.');
    }
  }

  async function assignNutritionist(reservaItem: Reserva) {
    if (!reservaItem.nutricionistaTemp) return setNotice('Selecciona un nutricionista de la lista.');
    try {
      await clinicApi.asignarNutricionista(reservaItem);
      setReservas((current) => current.map((item) => item.id === reservaItem.id
        ? { ...item, nutricionista: reservaItem.nutricionistaTemp }
        : item));
      setNotice('¡Nutricionista asignado con éxito!');
    } catch (error) {
      console.error('Error al asignar nutricionista:', error);
      setNotice('Hubo un error al guardar la asignación.');
    }
  }

  const publicView = role !== 'Nutricionista' && role !== 'Secretaria' && role !== 'Administrador';

  if (role === 'Administrador') return <Navigate to="/admin" replace />;

  return (
    <SiteLayout>
      {notice && <div className="notice" role="status"><span>{notice}</span><button onClick={() => setNotice('')} aria-label="Cerrar aviso">×</button></div>}
      {publicView && <>
        <main>
          <section className="hero">
            <div className="hero-copy">
              <p className="eyebrow">Nutrición clínica · Temuco</p>
              <h1>Alimenta tu bienestar, <em>un hábito a la vez.</em></h1>
              <p>Atención nutricional cercana y personalizada para acompañarte en cada etapa de tu salud.</p>
              <a className="button button-lime" href={isAuthenticated ? '#reservas' : '/iniciar-sesion'}>{isAuthenticated ? 'Reserva tu hora' : 'Conoce nuestros servicios'} <span aria-hidden="true">↗</span></a>
              <div className="hero-note"><span className="note-mark">N</span><span>Un equipo que te escucha<br /><b>desde 2016 en Temuco</b></span></div>
            </div>
            <div className="hero-image"><img src={nutritionPhoto} alt="Profesional de salud preparando una consulta de nutrición" /><span className="image-caption">Pequeñas decisiones, grandes cambios.</span></div>
          </section>

          <section id="servicios" className="services-section content-section">
            <div className="section-heading"><div><p className="eyebrow">Atención personalizada</p><h2>Tu salud, a tu manera.</h2></div><p>Planes pensados para tu historia, tus objetivos y tu ritmo de vida.</p></div>
            <div className="services-grid">
              <article className="service-card"><img src={consultationPhoto} alt="Nutricionista conversando con una paciente" /><div><span>01 / SALUD</span><h3>Pérdida de peso</h3><p>Acompañamiento realista para construir cambios sostenibles.</p></div></article>
              <article className="service-card"><div className="service-color-block"><span>02 / PREVENCIÓN</span><b>Bienestar<br />metabólico</b></div><div><h3>Enfermedades metabólicas</h3><p>Orientación nutricional para cuidar tu salud a largo plazo.</p></div></article>
              <article className="service-card"><img src={movementPhoto} alt="Deportista estirando antes de entrenar" /><div><span>03 / MOVIMIENTO</span><h3>Nutrición deportiva</h3><p>Planes enfocados en energía, recuperación y rendimiento.</p></div></article>
              <article className="service-card"><div className="service-color-block service-color-light"><span>04 / ELECCIÓN</span><b>Comer bien<br />a tu estilo</b></div><div><h3>Alimentación veggie</h3><p>Opciones vegetarianas y veganas completas y equilibradas.</p></div></article>
            </div>
          </section>

          {isAuthenticated && role === 'Paciente' && <section id="reservas" className="booking-section content-section">
            <div className="booking-intro"><p className="eyebrow">Da el primer paso</p><h2>Reserva tu próxima consulta.</h2><p>Elige el tipo de atención y horario que mejor se adapte a ti.</p><img src={consultationPhoto} alt="Consulta nutricional personalizada" /></div>
            <form className="booking-form" onSubmit={submitReservation}>
              <h3>Solicitar una hora</h3>
              <div className="field-grid">
                <label>Tipo de atención<select required value={reserva.tipoAtencion} onChange={(event) => setReserva({ ...reserva, tipoAtencion: event.target.value })}><option value="">Selecciona</option><option>Control</option><option>Primera vez</option></select></label>
                <label>Modalidad<select required value={reserva.modalidad} onChange={(event) => setReserva({ ...reserva, modalidad: event.target.value })}><option value="">Selecciona</option><option>Presencial</option><option>Online</option></select></label>
                <label>Fecha<input required type="date" min={new Date().toISOString().slice(0, 10)} value={reserva.fecha} onChange={(event) => setReserva({ ...reserva, fecha: event.target.value })} /></label>
                <label>Hora<input required type="time" min="09:00" max="20:00" value={reserva.hora} onChange={(event) => setReserva({ ...reserva, hora: event.target.value })} /></label>
              </div>
              <div className="form-actions"><button className="button button-green" type="submit">Reservar hora <span>↗</span></button><button className="button button-text" type="reset" onClick={() => setReserva({ tipoAtencion: '', modalidad: '', fecha: '', hora: '', pacienteNombre: userName })}>Limpiar</button></div>
            </form>
          </section>}

          <section id="mapa" className="location-section content-section">
            <div><p className="eyebrow">Estamos cerca</p><h2>Conversemos en Temuco.</h2><p className="location-lede">Un espacio para que te sientas acompañado en tu camino hacia una vida más saludable.</p><div className="location-details"><p><b>Dirección</b>Av. Alemania 0281, Temuco</p><p><b>Horario</b>Lunes a sábado · 09:00 a 20:00</p><p><b>Teléfono</b>+56 9 1234 5678</p></div></div>
            <div className="map-frame"><iframe title="Ubicación de Clínica NutriVida en Temuco" src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3119.5!2d-72.59!3d-38.73!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x0%3A0x0!2zMzjCsDQzJzQ4LjAiUyA3MsKwMzUnMjQuMCJX!5e0!3m2!1ses!2scl!4v1650000000000!5m2!1ses!2scl" loading="lazy" referrerPolicy="no-referrer-when-downgrade" /></div>
          </section>
        </main>
      </>}

      {isAuthenticated && role === 'Nutricionista' && <main className="workspace content-section"><div className="section-heading"><div><p className="eyebrow">Área profesional</p><h1>Panel de nutricionista</h1></div><p>Gestión de pacientes y fichas nutricionales.</p></div><section className="workspace-panel"><h2>Nueva ficha nutricional</h2><form onSubmit={submitFicha} className="field-grid ficha-form">
        <label>RUT del paciente<input required value={nuevaFicha.pacienteRut} placeholder="11.111.111-1" onChange={(event) => setNuevaFicha({ ...nuevaFicha, pacienteRut: event.target.value })} /></label>
        <label>Motivo de consulta<input required value={nuevaFicha.motivoConsulta} onChange={(event) => setNuevaFicha({ ...nuevaFicha, motivoConsulta: event.target.value })} /></label>
        <label>Antecedentes médicos<input value={nuevaFicha.antecedentes} onChange={(event) => setNuevaFicha({ ...nuevaFicha, antecedentes: event.target.value })} /></label>
        <label>Peso actual (kg)<input required min="1" type="number" value={nuevaFicha.pesoActual ?? ''} onChange={(event) => setNuevaFicha({ ...nuevaFicha, pesoActual: Number(event.target.value) || null })} /></label>
        <button className="button button-green" type="submit">Guardar ficha <span>↗</span></button>
      </form></section><section className="workspace-panel"><h2>Fichas registradas</h2><div className="table-scroll"><table><thead><tr><th>ID</th><th>RUT paciente</th><th>Motivo de consulta</th><th>Antecedentes</th></tr></thead><tbody>{fichas.map((ficha) => <tr key={ficha.id}><td>{ficha.id}</td><td>{ficha.pacienteRut}</td><td>{ficha.motivoConsulta}</td><td>{ficha.antecedentes}</td></tr>)}</tbody></table></div>{fichas.length === 0 && <p className="empty-state">No hay fichas disponibles.</p>}</section></main>}

      {isAuthenticated && role === 'Secretaria' && <main className="workspace content-section"><div className="section-heading"><div><p className="eyebrow">Área de recepción</p><h1>Reservas de pacientes</h1></div><button className="button button-green" onClick={refreshReservations}>Actualizar lista <span>↻</span></button></div><section className="workspace-panel"><div className="table-scroll"><table><thead><tr><th>Paciente</th><th>Fecha</th><th>Hora</th><th>Atención</th><th>Nutricionista</th><th>Acción</th></tr></thead><tbody>{reservas.map((item) => <tr key={item.id}><td>{item.pacienteNombre}</td><td>{item.fecha}</td><td>{item.hora}</td><td>{item.tipoAtencion}</td><td>{item.nutricionista ? <strong>{item.nutricionista}</strong> : <select aria-label={`Asignar nutricionista a ${item.pacienteNombre}`} value={item.nutricionistaTemp ?? ''} onChange={(event) => setReservas((current) => current.map((row) => row.id === item.id ? { ...row, nutricionistaTemp: event.target.value } : row))}><option value="">Selecciona</option>{nutritionists.map((nutritionist) => <option key={nutritionist.id} value={nutritionist.nombre}>{nutritionist.nombre}</option>)}</select>}</td><td>{item.nutricionista ? <span className="assigned">Asignado</span> : <button className="button button-small" onClick={() => assignNutritionist(item)}>Confirmar</button>}</td></tr>)}</tbody></table></div>{reservas.length === 0 && <p className="empty-state">No hay reservas por mostrar.</p>}</section></main>}
    </SiteLayout>
  );
}

function AdminPage() {
  const { accounts } = useMsal();
  const isAuthenticated = useIsAuthenticated();
  const account = accounts[0] ?? null;
  const role = roleFor(account?.username ?? '');
  const [nutritionists, setNutritionists] = useState<Nutricionista[]>([]);
  const [reservas, setReservas] = useState<Reserva[]>([]);
  const [form, setForm] = useState({ nombre: '', especialidad: '', email: '' });
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    refreshAdminData();
  }, []);

  async function refreshAdminData() {
    setLoading(true);
    try {
      const [loadedNutritionists, loadedReservas] = await Promise.all([
        clinicApi.nutricionistas(),
        clinicApi.reservas()
      ]);
      setNutritionists(loadedNutritionists);
      setReservas(loadedReservas);
      setNotice('');
    } catch (error) {
      console.error('Error al cargar el panel de administración:', error);
      setNotice('No se pudieron cargar los datos. Verifica que el backend esté disponible.');
    } finally {
      setLoading(false);
    }
  }

  async function addNutritionist(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!form.nombre || !form.especialidad || !form.email) return;
    try {
      const savedNutritionist = await clinicApi.crearNutricionista({
        nombre: form.nombre.trim(),
        especialidad: form.especialidad.trim(),
        email: form.email.trim()
      });
      setNutritionists((current) => [...current, savedNutritionist]);
      setForm({ nombre: '', especialidad: '', email: '' });
      setNotice('Nutricionista agregado.');
    } catch (error) {
      console.error('Error al crear nutricionista:', error);
      setNotice('No se pudo guardar el nutricionista.');
    }
  }

  async function removeNutritionist(id: number | undefined) {
    if (id === undefined) return;
    try {
      await clinicApi.eliminarNutricionista(id);
      setNutritionists((current) => current.filter((item) => item.id !== id));
      setNotice('Nutricionista eliminado.');
    } catch (error) {
      console.error('Error al eliminar nutricionista:', error);
      setNotice('No se pudo eliminar el nutricionista.');
    }
  }

  if (!isAuthenticated || role !== 'Administrador') return <Navigate to="/" replace />;

  return (
    <SiteLayout>
      <main className="admin-page content-section">
        <div className="section-heading admin-heading">
          <div>
            <p className="eyebrow">Administración</p>
            <h1>Panel del administrador</h1>
          </div>
          <button className="button button-outline" type="button" onClick={refreshAdminData}>Actualizar datos</button>
        </div>
        {notice && <p className="admin-notice" role="status">{notice}</p>}

        <section className="admin-summary-grid">
          <article className="admin-stat">
            <span>Reservas registradas</span>
            <strong>{loading ? '...' : reservas.length}</strong>
            <small>Datos guardados en el sistema</small>
          </article>
          <article className="admin-stat">
            <span>Tokens de Azure</span>
            <strong className="admin-stat-text">No disponible</strong>
            <small>El backend aún no registra consumo de tokens</small>
          </article>
          <article className="admin-stat">
            <span>Nutricionistas</span>
            <strong>{loading ? '...' : nutritionists.length}</strong>
            <small>Profesionales registrados</small>
          </article>
        </section>

        <section className="admin-grid">
          <div className="workspace-panel admin-card">
            <h2>Agregar nutricionista</h2>
            <form className="field-grid admin-form" onSubmit={addNutritionist}>
              <label>Nombre<input required value={form.nombre} onChange={(event) => setForm({ ...form, nombre: event.target.value })} placeholder="Ej: Dra. Navarro" /></label>
              <label>Especialidad<input required value={form.especialidad} onChange={(event) => setForm({ ...form, especialidad: event.target.value })} placeholder="Ej: Nutrición infantil" /></label>
              <label className="full-width">Correo<input required type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} placeholder="ejemplo@nutrivida.cl" /></label>
              <button className="button button-green full-width-button" type="submit">Agregar nutricionista</button>
            </form>
          </div>

          <div className="workspace-panel admin-card">
            <h2>Nutricionistas</h2>
            <div className="admin-list">
              {nutritionists.map((item) => (
                <div key={item.id} className="admin-list-item">
                  <div>
                    <strong>{item.nombre}</strong>
                    <small>{item.especialidad}</small>
                    <span>{item.email}</span>
                  </div>
                  <button className="button button-outline" type="button" onClick={() => removeNutritionist(item.id)}>Eliminar</button>
                </div>
              ))}
              {!loading && nutritionists.length === 0 && <p className="empty-state">No hay nutricionistas registrados.</p>}
            </div>
          </div>
        </section>

        <section className="workspace-panel admin-history">
          <h2>Historial de reservas y consultas</h2>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Paciente</th>
                  <th>Nutricionista</th>
                  <th>Fecha</th>
                  <th>Hora</th>
                  <th>Tipo</th>
                  <th>Modalidad</th>
                  <th>Tokens</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {reservas.map((reserva) => (
                  <tr key={reserva.id}>
                    <td>{reserva.pacienteNombre}</td>
                    <td>{reserva.nutricionista || 'Sin asignar'}</td>
                    <td>{reserva.fecha}</td>
                    <td>{reserva.hora}</td>
                    <td>{reserva.tipoAtencion}</td>
                    <td>{reserva.modalidad}</td>
                    <td>No medido</td>
                    <td>{reserva.estado ?? 'Pendiente'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!loading && reservas.length === 0 && <p className="empty-state">No hay reservas registradas.</p>}
        </section>
      </main>
    </SiteLayout>
  );
}

function LoginPage() {
  const { instance } = useMsal();
  const isAuthenticated = useIsAuthenticated();
  const navigate = useNavigate();
  const [error, setError] = useState('');

  if (isAuthenticated) return <Navigate to="/" replace />;

  async function signIn() {
    try {
      const accountInfo = await instance.loginPopup({ scopes: ['user.read'] });
      const nextPath = roleFor(accountInfo.account.username ?? '') === 'Administrador' ? '/admin' : '/';
      navigate(nextPath);
    } catch (loginError) {
      console.error('Error al iniciar sesión:', loginError);
      setError('No se pudo iniciar sesión. Intenta nuevamente.');
    }
  }

  return <SiteLayout><main className="login-page"><div className="login-image"><img src={nutritionPhoto} alt="Profesional de nutrición en consulta" /><div><p className="eyebrow">Un buen comienzo</p><h1>Tu bienestar empieza con una conversación.</h1></div></div><section className="login-panel"><p className="eyebrow">Bienvenido a NutriVida</p><h2>Inicia sesión</h2><p>Accede de forma segura para reservar tu consulta o gestionar tu atención.</p><button className="button button-green login-button" onClick={signIn}><span className="microsoft-mark" aria-hidden="true">▦</span>Continuar con Microsoft</button>{error && <p className="login-error" role="alert">{error}</p>}<Link className="back-link" to="/">← Volver al inicio</Link></section></main></SiteLayout>;
}

export default function App() {
  return <Routes><Route path="/" element={<HomePage />} /><Route path="/admin" element={<AdminPage />} /><Route path="/iniciar-sesion" element={<LoginPage />} /><Route path="*" element={<Navigate to="/" replace />} /></Routes>;
}
