import React, { useEffect, useMemo, useState } from "react";
import {
  SafeAreaView, ScrollView,
  StyleSheet,
  Text, TextInput, TouchableOpacity,
  View
} from "react-native";
// ── No external storage lib needed ─────────────────────────────────────────
// Data lives in memory while the app is open.
// ── Types ──────────────────────────────────────────────────────────────────
type ToastType   = "success" | "error" | "info";
type NotifType   = "success" | "error" | "info";
type ApptStatus  = "pending" | "completed" | "cancelled";
type Role        = "admin" | "doctor";
type TabId       = "home" | "patients" | "doctors" | "appointments" | "notifications";
type ScreenId    = "menu" | "regPatient" | "addDoctor" | "assign" | "book";
type SortOption  = "name" | "id" | "age" | "appts";

interface ToastItem    { id: number; msg: string; type: ToastType; }
interface NotifItem    { id: number; msg: string; type: NotifType; time: string; read: boolean; }
interface Appointment  { apptId: string; patientName: string; doctorName: string; doctorId: string; date: string; time: string; status: ApptStatus; }
interface Patient      { id: string; name: string; age: string; disease: string; assignedDoctorId: string; }
interface Doctor       { id: string; name: string; age: string; specialization: string; appointments: Appointment[]; }
interface StoredState  { patients: Patient[]; doctors: Doctor[]; notifications: NotifItem[]; }

// ── Constants ──────────────────────────────────────────────────────────────
const STORAGE_KEY = "mountKigaliHospital_v3";

const C = {
  bg:      "#0f172a",
  card:    "#1e293b",
  border:  "#1e3a5f",
  text:    "#f1f5f9",
  muted:   "#475569",
  subtle:  "#94a3b8",
  green:   "#4ade80",
  blue:    "#38bdf8",
  purple:  "#c084fc",
  yellow:  "#fbbf24",
  red:     "#f87171",
  orange:  "#fb923c",
};

const STATUS_COLORS: Record<ApptStatus, { bg: string; border: string; text: string }> = {
  pending:   { bg: C.yellow + "30", border: C.yellow, text: C.yellow },
  completed: { bg: C.green  + "30", border: C.green,  text: C.green  },
  cancelled: { bg: C.red    + "30", border: C.red,    text: C.red    },
};

const CHART_COLORS = [C.green, C.blue, C.purple, C.orange, "#f472b6", "#34d399", "#a78bfa", C.yellow];

// ── Validation ─────────────────────────────────────────────────────────────
const isValidAge  = (v: string) => { const n = parseInt(v, 10); return !isNaN(n) && n > 0 && n < 150; };
const isValidId   = (v: string) => /^[A-Za-z0-9]+$/.test(v.trim());
const isValidDate = (v: string) => /^\d{4}-\d{2}-\d{2}$/.test(v) && !isNaN(Date.parse(v));
const isValidTime = (v: string) => /^\d{1,2}:\d{2}\s*(AM|PM)$/i.test(v.trim());

// ── Storage (in-memory — no install needed) ────────────────────────────────
const _store: Record<string, string> = {};
async function loadData(): Promise<StoredState | null> {
  try { const raw = _store[STORAGE_KEY]; return raw ? (JSON.parse(raw) as StoredState) : null; } catch { return null; }
}
async function saveData(state: StoredState): Promise<void> {
  try { _store[STORAGE_KEY] = JSON.stringify(state); } catch {}
}

// ── Small components ───────────────────────────────────────────────────────
function SectionTitle({ children }: { children: string }) {
  return <Text style={s.sectionTitle}>{children}</Text>;
}

function Badge({ text, color }: { text: string; color: string }) {
  return (
    <View style={[s.badge, { backgroundColor: color + "25", borderColor: color + "55" }]}>
      <Text style={[s.badgeText, { color }]}>{text}</Text>
    </View>
  );
}

function Divider() {
  return <View style={s.divider} />;
}

function EmptyState({ icon, text }: { icon: string; text: string }) {
  return (
    <View style={s.emptyState}>
      <Text style={s.emptyIcon}>{icon}</Text>
      <Text style={s.emptyText}>{text}</Text>
    </View>
  );
}

function CardRow({ label, value, valueColor }: { label: string; value: string | number; valueColor?: string }) {
  return (
    <View style={s.cardRow}>
      <Text style={s.cardKey}>{label}</Text>
      <Text style={[s.cardVal, valueColor ? { color: valueColor } : null]}>{value}</Text>
    </View>
  );
}

function Field({
  label, value, onChangeText, placeholder, keyboardType, secureTextEntry,
}: {
  label: string;
  value: string;
  onChangeText: (t: string) => void;
  placeholder: string;
  keyboardType?: "default" | "numeric";
  secureTextEntry?: boolean;
}) {
  return (
    <View style={{ marginBottom: 14 }}>
      <Text style={s.fieldLabel}>{label}</Text>
      <TextInput
        style={s.input}
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={C.muted}
        keyboardType={keyboardType || "default"}
        secureTextEntry={secureTextEntry}
        autoCapitalize="none"
      />
    </View>
  );
}

function SubmitBtn({ label, onPress, color }: { label: string; onPress: () => void; color?: string }) {
  return (
    <TouchableOpacity style={[s.submitBtn, { backgroundColor: color || C.blue }]} onPress={onPress} activeOpacity={0.8}>
      <Text style={[s.submitBtnText, { color: color ? C.text : C.bg }]}>{label}</Text>
    </TouchableOpacity>
  );
}

function MenuCard({ color, icon, label, onPress }: { color: string; icon: string; label: string; onPress: () => void }) {
  return (
    <TouchableOpacity style={[s.menuCard, { borderTopColor: color }]} onPress={onPress} activeOpacity={0.75}>
      <Text style={s.menuIcon}>{icon}</Text>
      <Text style={s.menuLabel}>{label}</Text>
    </TouchableOpacity>
  );
}

function StatCard({ icon, num, label, color }: { icon: string; num: number; label: string; color: string }) {
  return (
    <View style={s.statCard}>
      <Text style={{ fontSize: 20 }}>{icon}</Text>
      <Text style={[s.statNum, { color }]}>{num}</Text>
      <Text style={s.statLabel}>{label}</Text>
    </View>
  );
}

function BarChart({ entries, maxVal }: { entries: [string, number][]; maxVal: number }) {
  if (!entries.length) return <EmptyState icon="📊" text="No data yet." />;
  return (
    <View style={{ rowGap: 10, columnGap: 10 }}>
      {entries.map(([label, count], i) => (
        <View key={label}>
          <View style={s.barRow}>
            <Text style={s.barLabel}>{label}</Text>
            <Text style={[s.barCount, { color: CHART_COLORS[i % CHART_COLORS.length] }]}>{count}</Text>
          </View>
          <View style={s.barTrack}>
            <View style={[s.barFill, {
              width: maxVal > 0 ? `${(count / maxVal) * 100}%` as any : "0%",
              backgroundColor: CHART_COLORS[i % CHART_COLORS.length],
            }]} />
          </View>
        </View>
      ))}
    </View>
  );
}

// ── Toast ──────────────────────────────────────────────────────────────────
function ToastBar({ toasts }: { toasts: ToastItem[] }) {
  if (!toasts.length) return null;
  const colors: Record<ToastType, string> = { success: C.green, error: C.red, info: C.blue };
  const icons:  Record<ToastType, string> = { success: "✓", error: "✗", info: "🔔" };
  return (
    <View style={s.toastContainer} pointerEvents="none">
      {toasts.map(t => (
        <View key={t.id} style={[s.toast, { backgroundColor: colors[t.type] + "20", borderColor: colors[t.type] + "88" }]}>
          <Text style={[s.toastText, { color: colors[t.type] }]}>{icons[t.type]} {t.msg}</Text>
        </View>
      ))}
    </View>
  );
}

// ── Sort/Filter pill row ───────────────────────────────────────────────────
function PillRow<T extends string>({ options, value, onChange }: { options: { label: string; value: T }[]; value: T; onChange: (v: T) => void }) {
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ paddingVertical: 6 }}>
      {options.map(o => (
        <TouchableOpacity key={o.value} onPress={() => onChange(o.value)} style={[s.pill, value === o.value && s.pillActive]}>
          <Text style={[s.pillText, value === o.value && s.pillTextActive]}>{o.label}</Text>
        </TouchableOpacity>
      ))}
    </ScrollView>
  );
}

// ── Login Screen ───────────────────────────────────────────────────────────
function LoginScreen({ onLogin }: { onLogin: (role: Role) => void }) {
  const [role, setRole] = useState<Role>("admin");
  const [pass, setPass] = useState("");
  const [err,  setErr]  = useState("");

  function submit() {
    const creds: Record<Role, string> = { admin: "admin123", doctor: "doc123" };
    if (pass === creds[role]) { onLogin(role); }
    else { setErr("Incorrect password."); setTimeout(() => setErr(""), 2500); }
  }

  return (
    <SafeAreaView style={s.safeArea}>
      <ScrollView contentContainerStyle={s.loginScroll}>
        <View style={s.loginCard}>
          <Text style={s.loginIcon}>🏥</Text>
          <Text style={s.loginTitle}>Mount Kigali General</Text>
          <Text style={s.loginSub}>Hospital Management System</Text>

          <View style={s.roleRow}>
            {(["admin", "doctor"] as Role[]).map(r => (
              <TouchableOpacity key={r} onPress={() => setRole(r)} style={[s.roleBtn, role === r && s.roleBtnActive]}>
                <Text style={[s.roleBtnText, role === r && s.roleBtnTextActive]}>
                  {r === "admin" ? "👑 Admin" : "👨‍⚕️ Doctor"}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          <Field label="PASSWORD" value={pass} onChangeText={setPass} placeholder={role === "admin" ? "admin123" : "doc123"} secureTextEntry />
          {!!err && <Text style={s.errMsg}>{err}</Text>}
          <SubmitBtn label="Login →" onPress={submit} />
          <Text style={s.loginHint}>admin: admin123  |  doctor: doc123</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

// ── Main App ───────────────────────────────────────────────────────────────
export default function App() {
  const [user,          setUser]          = useState<Role | null>(null);
  const [patients,      setPatients]      = useState<Patient[]>([]);
  const [doctors,       setDoctors]       = useState<Doctor[]>([]);
  const [notifications, setNotifications] = useState<NotifItem[]>([]);
  const [toasts,        setToasts]        = useState<ToastItem[]>([]);

  const [tab,          setTab]          = useState<TabId>("home");
  const [screen,       setScreen]       = useState<ScreenId>("menu");
  const [searchQ,      setSearchQ]      = useState("");
  const [sortBy,       setSortBy]       = useState<SortOption>("name");
  const [filterStatus, setFilterStatus] = useState<"all" | ApptStatus>("all");

  // Patient form
  const [pId,setPId]=useState(""); const [pName,setPName]=useState("");
  const [pAge,setPAge]=useState(""); const [pDis,setPDis]=useState("");
  // Doctor form
  const [dId,setDId]=useState(""); const [dName,setDName]=useState("");
  const [dAge,setDAge]=useState(""); const [dSpec,setDSpec]=useState("");
  // Assign form
  const [aPId,setAPId]=useState(""); const [aDId,setADId]=useState("");
  // Appointment form
  const [apId,setApId]=useState(""); const [apPat,setApPat]=useState("");
  const [apDoc,setApDoc]=useState(""); const [apDate,setApDate]=useState("");
  const [apTime,setApTime]=useState("");

  // Load persisted data
  useEffect(() => {
    loadData().then(saved => {
      if (saved) {
        setPatients(saved.patients || []);
        setDoctors(saved.doctors   || []);
        setNotifications(saved.notifications || []);
      }
    });
  }, []);

  // Auto-save
  useEffect(() => {
    if (user) saveData({ patients, doctors, notifications });
  }, [patients, doctors, notifications, user]);

  function toast(msg: string, type: ToastType = "success") {
    const id = Date.now() + Math.random();
    setToasts(prev => [...prev, { id, msg, type }]);
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 3500);
  }
  function notify(msg: string, type: NotifType = "info") {
    setNotifications(prev => [{
      id: Date.now() + Math.random(), msg, type,
      time: new Date().toLocaleTimeString(), read: false,
    }, ...prev.slice(0, 49)]);
  }

  const isAdmin = user === "admin";

  // ── Handlers ──────────────────────────────────────────────────────────────
  function registerPatient() {
    if (!pId||!pName||!pAge||!pDis)  { toast("All fields required.","error"); return; }
    if (!isValidId(pId))              { toast("ID: letters & numbers only.","error"); return; }
    if (!isValidAge(pAge))            { toast("Age must be 1–149.","error"); return; }
    if (patients.find(p=>p.id===pId.trim())) { toast("Patient ID exists.","error"); return; }
    const p: Patient = { id:pId.trim(), name:pName.trim(), age:pAge, disease:pDis.trim(), assignedDoctorId:"Not Assigned" };
    setPatients(prev=>[...prev,p]);
    toast(`${p.name} registered.`,"success");
    notify(`New patient: ${p.name} (${p.id})`,"success");
    setPId(""); setPName(""); setPAge(""); setPDis("");
  }

  function addDoctor() {
    if (!dId||!dName||!dAge||!dSpec)  { toast("All fields required.","error"); return; }
    if (!isValidId(dId))              { toast("ID: letters & numbers only.","error"); return; }
    if (!isValidAge(dAge))            { toast("Age must be 1–149.","error"); return; }
    if (doctors.find(d=>d.id===dId.trim())) { toast("Doctor ID exists.","error"); return; }
    const d: Doctor = { id:dId.trim(), name:dName.trim(), age:dAge, specialization:dSpec.trim(), appointments:[] };
    setDoctors(prev=>[...prev,d]);
    toast(`Dr. ${d.name} added.`,"success");
    notify(`New doctor: Dr. ${d.name} – ${d.specialization}`,"info");
    setDId(""); setDName(""); setDAge(""); setDSpec("");
  }

  function assignDoctor() {
    if (!aPId||!aDId) { toast("All fields required.","error"); return; }
    const patient = patients.find(p=>p.id===aPId.trim());
    const doctor  = doctors.find(d=>d.id===aDId.trim());
    if (!patient) { toast(`Patient "${aPId}" not found.`,"error"); return; }
    if (!doctor)  { toast(`Doctor "${aDId}" not found.`,"error"); return; }
    setPatients(prev=>prev.map(p=>p.id===patient.id?{...p,assignedDoctorId:doctor.id}:p));
    toast(`Dr. ${doctor.name} → ${patient.name}.`,"success");
    notify(`Dr. ${doctor.name} assigned to ${patient.name}`,"success");
    setAPId(""); setADId("");
  }

  function bookAppointment() {
    if (!apId||!apPat||!apDoc||!apDate||!apTime) { toast("All fields required.","error"); return; }
    if (!isValidDate(apDate)) { toast("Date must be YYYY-MM-DD.","error"); return; }
    if (!isValidTime(apTime)) { toast("Time must be HH:MM AM/PM.","error"); return; }
    const patient = patients.find(p=>p.id===apPat.trim());
    const doctor  = doctors.find(d=>d.id===apDoc.trim());
    if (!patient) { toast(`Patient "${apPat}" not found.`,"error"); return; }
    if (!doctor)  { toast(`Doctor "${apDoc}" not found.`,"error"); return; }
    const conflict = doctor.appointments.find(a=>a.date===apDate&&a.time.toLowerCase()===apTime.trim().toLowerCase());
    if (conflict) { toast(`Dr. ${doctor.name} already booked at that time.`,"error"); return; }
    const appt: Appointment = { apptId:apId.trim(), patientName:patient.name, doctorName:doctor.name, doctorId:doctor.id, date:apDate, time:apTime.trim(), status:"pending" };
    setDoctors(prev=>prev.map(d=>d.id===doctor.id?{...d,appointments:[...d.appointments,appt]}:d));
    toast("Appointment booked!","success");
    notify(`Appt: ${patient.name} with Dr. ${doctor.name} on ${apDate}`,"info");
    setApId(""); setApPat(""); setApDoc(""); setApDate(""); setApTime("");
  }

  function updateStatus(doctorId: string, apptId: string, status: ApptStatus) {
    setDoctors(prev=>prev.map(d=>
      d.id===doctorId ? {...d,appointments:d.appointments.map(a=>a.apptId===apptId?{...a,status}:a)} : d
    ));
    toast(`Marked as ${status}.`,"success");
    notify(`Appointment ${apptId} → ${status}`,"info");
  }

  // ── Derived data ───────────────────────────────────────────────────────────
  const allAppts = useMemo<Appointment[]>(()=>doctors.flatMap(d=>d.appointments),[doctors]);

  const filteredPatients = useMemo<Patient[]>(()=>{
    const q=searchQ.toLowerCase();
    let list=patients.filter(p=>p.name.toLowerCase().includes(q)||p.id.toLowerCase().includes(q)||p.disease.toLowerCase().includes(q));
    if(sortBy==="name") list=[...list].sort((a,b)=>a.name.localeCompare(b.name));
    if(sortBy==="age")  list=[...list].sort((a,b)=>Number(a.age)-Number(b.age));
    if(sortBy==="id")   list=[...list].sort((a,b)=>a.id.localeCompare(b.id));
    return list;
  },[patients,searchQ,sortBy]);

  const filteredDoctors = useMemo<Doctor[]>(()=>{
    const q=searchQ.toLowerCase();
    let list=doctors.filter(d=>d.name.toLowerCase().includes(q)||d.id.toLowerCase().includes(q)||d.specialization.toLowerCase().includes(q));
    if(sortBy==="name")  list=[...list].sort((a,b)=>a.name.localeCompare(b.name));
    if(sortBy==="appts") list=[...list].sort((a,b)=>b.appointments.length-a.appointments.length);
    return list;
  },[doctors,searchQ,sortBy]);

  const filteredAppts = useMemo<Appointment[]>(()=>{
    const q=searchQ.toLowerCase();
    return allAppts.filter(a=>
      (filterStatus==="all"||a.status===filterStatus)&&
      (a.patientName.toLowerCase().includes(q)||a.doctorName.toLowerCase().includes(q)||a.apptId.toLowerCase().includes(q))
    );
  },[allAppts,filterStatus,searchQ]);

  const diseaseEntries = useMemo<[string,number][]>(()=>{
    const map: Record<string,number>={};
    patients.forEach(p=>{map[p.disease]=(map[p.disease]||0)+1;});
    return Object.entries(map).sort((a,b)=>b[1]-a[1]);
  },[patients]);

  const doctorApptEntries = useMemo<[string,number][]>(()=>
    [...doctors].sort((a,b)=>b.appointments.length-a.appointments.length).map(d=>[`Dr. ${d.name}`,d.appointments.length])
  ,[doctors]);

  const unread = notifications.filter(n=>!n.read).length;
  const completedCount = allAppts.filter(a=>a.status==="completed").length;

  const showSearch = tab==="patients"||tab==="doctors"||tab==="appointments";

  // ── Auth gate ──────────────────────────────────────────────────────────────
  if (!user) return <LoginScreen onLogin={setUser} />;

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <SafeAreaView style={s.safeArea}>
      <ToastBar toasts={toasts} />

      {/* Header */}
      <View style={s.header}>
        <View>
          <Text style={s.headerTitle}>🏥 Mount Kigali General</Text>
          <Text style={s.headerSub}>Hospital Management System</Text>
        </View>
        <View style={s.headerRight}>
          <TouchableOpacity onPress={()=>setTab("notifications")} style={{ position:"relative" }}>
            <Text style={{ fontSize:22 }}>🔔</Text>
            {unread>0 && (
              <View style={s.badge_notif}>
                <Text style={s.badge_notif_text}>{unread}</Text>
              </View>
            )}
          </TouchableOpacity>
          <Badge text={isAdmin?"👑 Admin":"👨‍⚕️ Doctor"} color={isAdmin?C.yellow:C.blue} />
          <TouchableOpacity onPress={()=>setUser(null)} style={[s.logoutBtn]}>
            <Text style={s.logoutText}>Logout</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Search + sort */}
      {showSearch && (
        <View style={s.searchBar}>
          <TextInput
            style={s.searchInput}
            value={searchQ}
            onChangeText={setSearchQ}
            placeholder={`🔍  Search ${tab}…`}
            placeholderTextColor={C.muted}
          />
          {tab==="patients" && (
            <PillRow<SortOption>
              options={[{label:"Name",value:"name"},{label:"ID",value:"id"},{label:"Age",value:"age"}]}
              value={sortBy} onChange={setSortBy}
            />
          )}
          {tab==="doctors" && (
            <PillRow<SortOption>
              options={[{label:"Name",value:"name"},{label:"ID",value:"id"},{label:"Appts ↓",value:"appts"}]}
              value={sortBy} onChange={setSortBy}
            />
          )}
          {tab==="appointments" && (
            <PillRow<"all"|ApptStatus>
              options={[{label:"All",value:"all"},{label:"Pending",value:"pending"},{label:"Completed",value:"completed"},{label:"Cancelled",value:"cancelled"}]}
              value={filterStatus} onChange={setFilterStatus}
            />
          )}
        </View>
      )}

      {/* Content */}
      <ScrollView style={{ flex:1 }} contentContainerStyle={{ padding:16, paddingBottom:32 }}>

        {/* ── HOME ── */}
        {tab==="home" && (
          <>
            {screen==="menu" && (
              <>
                <SectionTitle>QUICK ACTIONS</SectionTitle>
                <View style={s.menuGrid}>
                  {isAdmin && <>
                    <MenuCard color={C.green}  icon="🧑‍⚕️" label="Register Patient" onPress={()=>setScreen("regPatient")} />
                    <MenuCard color={C.blue}   icon="👨‍⚕️" label="Add Doctor"       onPress={()=>setScreen("addDoctor")} />
                    <MenuCard color={C.yellow} icon="🔗"   label="Assign Doctor"   onPress={()=>setScreen("assign")} />
                  </>}
                  <MenuCard color={C.purple} icon="📅" label="Book Appointment" onPress={()=>setScreen("book")} />
                </View>

                <SectionTitle>OVERVIEW</SectionTitle>
                <View style={s.statsRow}>
                  <StatCard icon="🧑‍⚕️" num={patients.length} label="Patients"    color={C.green}  />
                  <StatCard icon="👨‍⚕️" num={doctors.length}  label="Doctors"     color={C.blue}   />
                  <StatCard icon="📅"   num={allAppts.length}  label="Appts"       color={C.purple} />
                  <StatCard icon="✅"   num={completedCount}   label="Done"        color={C.yellow} />
                </View>

                {isAdmin && <>
                  <SectionTitle>DISEASE DISTRIBUTION</SectionTitle>
                  <View style={s.card}>
                    <BarChart entries={diseaseEntries} maxVal={Math.max(...diseaseEntries.map(e=>e[1]),1)} />
                  </View>

                  <SectionTitle>APPOINTMENTS PER DOCTOR</SectionTitle>
                  <View style={s.card}>
                    <BarChart entries={doctorApptEntries} maxVal={Math.max(...doctorApptEntries.map(e=>e[1]),1)} />
                  </View>
                </>}
              </>
            )}

            {screen==="regPatient" && isAdmin && (
              <View>
                <TouchableOpacity onPress={()=>setScreen("menu")}><Text style={s.backBtn}>← Back</Text></TouchableOpacity>
                <SectionTitle>REGISTER NEW PATIENT</SectionTitle>
                <Field label="PATIENT ID" value={pId}   onChangeText={setPId}   placeholder="e.g. P001" />
                <Field label="FULL NAME"  value={pName} onChangeText={setPName} placeholder="e.g. Alice Uwimana" />
                <Field label="AGE"        value={pAge}  onChangeText={setPAge}  placeholder="e.g. 30" keyboardType="numeric" />
                <Field label="DISEASE"    value={pDis}  onChangeText={setPDis}  placeholder="e.g. Malaria" />
                <SubmitBtn label="Register Patient" onPress={registerPatient} />
              </View>
            )}
            {screen==="addDoctor" && isAdmin && (
              <View>
                <TouchableOpacity onPress={()=>setScreen("menu")}><Text style={s.backBtn}>← Back</Text></TouchableOpacity>
                <SectionTitle>ADD NEW DOCTOR</SectionTitle>
                <Field label="DOCTOR ID"     value={dId}   onChangeText={setDId}   placeholder="e.g. D001" />
                <Field label="FULL NAME"      value={dName} onChangeText={setDName} placeholder="e.g. Mugisha Jean" />
                <Field label="AGE"            value={dAge}  onChangeText={setDAge}  placeholder="e.g. 45" keyboardType="numeric" />
                <Field label="SPECIALIZATION" value={dSpec} onChangeText={setDSpec} placeholder="e.g. General Medicine" />
                <SubmitBtn label="Add Doctor" onPress={addDoctor} />
              </View>
            )}
            {screen==="assign" && isAdmin && (
              <View>
                <TouchableOpacity onPress={()=>setScreen("menu")}><Text style={s.backBtn}>← Back</Text></TouchableOpacity>
                <SectionTitle>ASSIGN DOCTOR TO PATIENT</SectionTitle>
                <Field label="PATIENT ID" value={aPId} onChangeText={setAPId} placeholder="e.g. P001" />
                <Field label="DOCTOR ID"  value={aDId} onChangeText={setADId} placeholder="e.g. D001" />
                <SubmitBtn label="Assign Doctor" onPress={assignDoctor} />
              </View>
            )}
            {screen==="book" && (
              <View>
                <TouchableOpacity onPress={()=>setScreen("menu")}><Text style={s.backBtn}>← Back</Text></TouchableOpacity>
                <SectionTitle>BOOK APPOINTMENT</SectionTitle>
                <Field label="APPOINTMENT ID"    value={apId}   onChangeText={setApId}   placeholder="e.g. A001" />
                <Field label="PATIENT ID"         value={apPat}  onChangeText={setApPat}  placeholder="e.g. P001" />
                <Field label="DOCTOR ID"          value={apDoc}  onChangeText={setApDoc}  placeholder="e.g. D001" />
                <Field label="DATE (YYYY-MM-DD)"  value={apDate} onChangeText={setApDate} placeholder="e.g. 2026-03-22" />
                <Field label="TIME (HH:MM AM/PM)" value={apTime} onChangeText={setApTime} placeholder="e.g. 10:00 AM" />
                <SubmitBtn label="Book Appointment" onPress={bookAppointment} />
              </View>
            )}
          </>
        )}

        {/* ── PATIENTS ── */}
        {tab==="patients" && (
          <>
            <SectionTitle>PATIENTS ({filteredPatients.length})</SectionTitle>
            {filteredPatients.length===0 && <EmptyState icon="🏥" text="No patients found." />}
            {filteredPatients.map(p=>(
              <View key={p.id} style={s.card}>
                <View style={s.cardHeader}>
                  <Text style={s.cardTitle}>{p.name}</Text>
                  <Badge text={p.id} color={C.green} />
                </View>
                <CardRow label="Age"     value={p.age} />
                <CardRow label="Disease" value={p.disease} />
                <Divider />
                <CardRow label="Assigned Doctor" value={p.assignedDoctorId}
                  valueColor={p.assignedDoctorId==="Not Assigned"?C.muted:C.blue} />
              </View>
            ))}
          </>
        )}

        {/* ── DOCTORS ── */}
        {tab==="doctors" && (
          <>
            <SectionTitle>DOCTORS ({filteredDoctors.length})</SectionTitle>
            {filteredDoctors.length===0 && <EmptyState icon="👨‍⚕️" text="No doctors found." />}
            {filteredDoctors.map(d=>(
              <View key={d.id} style={s.card}>
                <View style={s.cardHeader}>
                  <Text style={s.cardTitle}>Dr. {d.name}</Text>
                  <Badge text={d.id} color={C.blue} />
                </View>
                <CardRow label="Age"            value={d.age} />
                <CardRow label="Specialization" value={d.specialization} />
                {d.appointments.length>0 && (
                  <>
                    <Divider />
                    <Text style={s.apptSubTitle}>APPOINTMENTS ({d.appointments.length})</Text>
                    {d.appointments.map((a,i)=>{
                      const sc=STATUS_COLORS[a.status];
                      return (
                        <View key={i} style={s.apptCard}>
                          <View style={s.cardHeader}>
                            <Text style={s.cardVal}>{a.patientName}</Text>
                            <View style={{ flexDirection:"row", rowGap: 6, columnGap: 6 }}>
                              <View style={[s.statusPill,{backgroundColor:sc.bg,borderColor:sc.border}]}>
                                <Text style={[s.statusPillText,{color:sc.text}]}>{a.status}</Text>
                              </View>
                              <Badge text={a.apptId} color={C.purple} />
                            </View>
                          </View>
                          <Text style={s.apptTime}>{a.date} at {a.time}</Text>
                          {isAdmin && (
                            <View style={s.statusBtnRow}>
                              {(Object.keys(STATUS_COLORS) as ApptStatus[]).filter(st=>st!==a.status).map(st=>(
                                <TouchableOpacity key={st} onPress={()=>updateStatus(d.id,a.apptId,st)}
                                  style={[s.statusBtn,{backgroundColor:STATUS_COLORS[st].bg,borderColor:STATUS_COLORS[st].border}]}>
                                  <Text style={[s.statusBtnText,{color:STATUS_COLORS[st].text}]}>→ {st}</Text>
                                </TouchableOpacity>
                              ))}
                            </View>
                          )}
                        </View>
                      );
                    })}
                  </>
                )}
              </View>
            ))}
          </>
        )}

        {/* ── APPOINTMENTS ── */}
        {tab==="appointments" && (
          <>
            <SectionTitle>ALL APPOINTMENTS ({filteredAppts.length})</SectionTitle>
            {filteredAppts.length===0 && <EmptyState icon="📅" text="No appointments found." />}
            {filteredAppts.map((a,i)=>{
              const sc=STATUS_COLORS[a.status];
              return (
                <View key={i} style={s.card}>
                  <View style={s.cardHeader}>
                    <Text style={s.cardTitle}>{a.patientName}</Text>
                    <View style={[s.statusPill,{backgroundColor:sc.bg,borderColor:sc.border}]}>
                      <Text style={[s.statusPillText,{color:sc.text}]}>{a.status}</Text>
                    </View>
                  </View>
                  <CardRow label="Doctor"   value={"Dr. "+a.doctorName} />
                  <CardRow label="Date"     value={a.date} />
                  <CardRow label="Time"     value={a.time} />
                  <CardRow label="Appt. ID" value={a.apptId} valueColor={C.purple} />
                  {isAdmin && (
                    <View style={s.statusBtnRow}>
                      {(Object.keys(STATUS_COLORS) as ApptStatus[]).filter(st=>st!==a.status).map(st=>(
                        <TouchableOpacity key={st} onPress={()=>updateStatus(a.doctorId,a.apptId,st)}
                          style={[s.statusBtn,{backgroundColor:STATUS_COLORS[st].bg,borderColor:STATUS_COLORS[st].border}]}>
                          <Text style={[s.statusBtnText,{color:STATUS_COLORS[st].text}]}>Mark {st}</Text>
                        </TouchableOpacity>
                      ))}
                    </View>
                  )}
                </View>
              );
            })}
          </>
        )}

        {/* ── NOTIFICATIONS ── */}
        {tab==="notifications" && (
          <>
            <View style={{ flexDirection:"row", justifyContent:"space-between", alignItems:"center" }}>
              <SectionTitle>NOTIFICATIONS ({notifications.length})</SectionTitle>
              {notifications.length>0 && (
                <TouchableOpacity onPress={()=>setNotifications(prev=>prev.map(n=>({...n,read:true})))}>
                  <Text style={{ color:C.blue, fontSize:12, fontWeight:"600" }}>Mark all read</Text>
                </TouchableOpacity>
              )}
            </View>
            {notifications.length===0 && <EmptyState icon="🔔" text="No notifications yet." />}
            {notifications.map(n=>{
              const c=n.type==="success"?C.green:n.type==="error"?C.red:C.blue;
              return (
                <View key={n.id} style={[s.card,{borderLeftWidth:3,borderLeftColor:c,opacity:n.read?0.5:1}]}>
                  <View style={{ flexDirection:"row", justifyContent:"space-between" }}>
                    <Text style={[s.cardVal,{flex:1}]}>{n.msg}</Text>
                    {!n.read && (
                      <View style={[s.badge,{backgroundColor:c+"25",borderColor:c}]}>
                        <Text style={[s.badgeText,{color:c}]}>NEW</Text>
                      </View>
                    )}
                  </View>
                  <Text style={[s.apptTime,{marginTop:6}]}>{n.time}</Text>
                </View>
              );
            })}
          </>
        )}

      </ScrollView>

      {/* ── Tab Bar ── */}
      <View style={s.tabBar}>
        {([
          {id:"home"          as TabId, icon:"🏠", label:"Home",     badge:0},
          {id:"patients"      as TabId, icon:"🧑‍⚕️", label:"Patients", badge:0},
          {id:"doctors"       as TabId, icon:"👨‍⚕️", label:"Doctors",  badge:0},
          {id:"appointments"  as TabId, icon:"📅", label:"Appts",    badge:0},
          {id:"notifications" as TabId, icon:"🔔", label:"Alerts",   badge:unread},
        ]).map(t=>(
          <TouchableOpacity key={t.id}
            style={[s.tabItem, tab===t.id && s.tabItemActive]}
            onPress={()=>{ setTab(t.id); setScreen("menu"); setSearchQ(""); }}>
            <View style={{ position:"relative" }}>
              <Text style={{ fontSize:20 }}>{t.icon}</Text>
              {t.badge>0 && (
                <View style={s.badge_notif}>
                  <Text style={s.badge_notif_text}>{t.badge}</Text>
                </View>
              )}
            </View>
            <Text style={[s.tabLabel, tab===t.id && s.tabLabelActive]}>{t.label}</Text>
          </TouchableOpacity>
        ))}
      </View>
    </SafeAreaView>
  );
}

// ── Styles ─────────────────────────────────────────────────────────────────
const s = StyleSheet.create({
  safeArea:        { flex:1, backgroundColor:C.bg },
  // Header
  header:          { backgroundColor:C.card, borderBottomWidth:1, borderBottomColor:C.border, padding:16, flexDirection:"row", justifyContent:"space-between", alignItems:"center" },
  headerTitle:     { color:C.text, fontSize:15, fontWeight:"800" },
  headerSub:       { color:C.muted, fontSize:10, marginTop:2 },
  headerRight:     { flexDirection:"row", rowGap: 10, columnGap: 10, alignItems:"center" },
  logoutBtn:       { backgroundColor:C.red+"20", borderWidth:1, borderColor:C.red, borderRadius:99, paddingHorizontal:12, paddingVertical:3 },
  logoutText:      { color:C.red, fontSize:11, fontWeight:"700" },
  // Search
  searchBar:       { backgroundColor:C.bg, borderBottomWidth:1, borderBottomColor:C.card, paddingHorizontal:16, paddingTop:10 },
  searchInput:     { backgroundColor:C.card, borderWidth:1, borderColor:"#334155", borderRadius:10, padding:10, color:C.text, fontSize:13 },
  // Pill
  pill:            { paddingHorizontal:12, paddingVertical:5, borderRadius:99, borderWidth:1, borderColor:"#334155", marginRight:8, marginTop:8 },
  pillActive:      { backgroundColor:C.blue+"20", borderColor:C.blue },
  pillText:        { color:C.muted, fontSize:11, fontWeight:"600" },
  pillTextActive:  { color:C.blue },
  // Login
  loginScroll:     { flexGrow:1, justifyContent:"center", padding:24 },
  loginCard:       { backgroundColor:C.card, borderWidth:1, borderColor:"#334155", borderRadius:20, padding:32, alignItems:"center" },
  loginIcon:       { fontSize:52, marginBottom:12 },
  loginTitle:      { color:C.text, fontSize:20, fontWeight:"800", marginBottom:4 },
  loginSub:        { color:C.muted, fontSize:12, marginBottom:24 },
  roleRow:         { flexDirection:"row", rowGap: 10, columnGap: 10, marginBottom:20, width:"100%" },
  roleBtn:         { flex:1, padding:10, borderWidth:1, borderColor:"#334155", borderRadius:10, alignItems:"center" },
  roleBtnActive:   { borderColor:C.blue, backgroundColor:C.blue+"15" },
  roleBtnText:     { color:C.subtle, fontSize:13, fontWeight:"600" },
  roleBtnTextActive:{ color:C.blue },
  loginHint:       { color:"#334155", fontSize:11, marginTop:14, textAlign:"center" },
  errMsg:          { color:C.red, fontSize:12, marginBottom:10, textAlign:"center" },
  // Section
  sectionTitle:    { color:C.muted, fontSize:10, fontWeight:"700", letterSpacing:1.5, marginBottom:10, marginTop:4, textTransform:"uppercase" },
  // Menu
  menuGrid:        { flexDirection:"row", flexWrap:"wrap", rowGap: 12, columnGap: 12, marginBottom:20 },
  menuCard:        { width:"47%", backgroundColor:C.card, borderRadius:16, padding:16, alignItems:"center", borderTopWidth:3, borderColor:C.border },
  menuIcon:        { fontSize:28, marginBottom:8 },
  menuLabel:       { color:C.text, fontSize:12, fontWeight:"600", textAlign:"center" },
  // Stats
  statsRow:        { flexDirection:"row", rowGap: 10, columnGap: 10, marginBottom:16 },
  statCard:        { flex:1, backgroundColor:C.card, borderRadius:14, padding:10, alignItems:"center", borderWidth:1, borderColor:C.border, rowGap: 2, columnGap: 2 },
  statNum:         { fontSize:20, fontWeight:"800" },
  statLabel:       { color:C.muted, fontSize:9 },
  // Card
  card:            { backgroundColor:C.card, borderRadius:14, padding:14, marginBottom:10, borderWidth:1, borderColor:C.border },
  cardHeader:      { flexDirection:"row", justifyContent:"space-between", alignItems:"center", marginBottom:8 },
  cardTitle:       { color:C.text, fontSize:14, fontWeight:"700" },
  cardRow:         { flexDirection:"row", justifyContent:"space-between", marginBottom:4 },
  cardKey:         { color:C.muted, fontSize:12 },
  cardVal:         { color:"#cbd5e1", fontSize:12, fontWeight:"600" },
  divider:         { height:1, backgroundColor:C.border, marginVertical:8 },
  // Appointment sub-card
  apptSubTitle:    { color:C.muted, fontSize:10, fontWeight:"700", letterSpacing:1, marginBottom:6, textTransform:"uppercase" },
  apptCard:        { backgroundColor:"#0f172a", borderRadius:8, padding:10, marginBottom:8 },
  apptTime:        { color:C.muted, fontSize:11, marginTop:3 },
  // Status
  statusPill:      { borderWidth:1, borderRadius:99, paddingHorizontal:8, paddingVertical:2 },
  statusPillText:  { fontSize:10, fontWeight:"700" },
  statusBtnRow:    { flexDirection:"row", rowGap: 6, columnGap: 6, marginTop:8, flexWrap:"wrap" },
  statusBtn:       { borderWidth:1, borderRadius:6, paddingHorizontal:10, paddingVertical:3 },
  statusBtnText:   { fontSize:10 },
  // Badge
  badge:           { borderWidth:1, borderRadius:99, paddingHorizontal:10, paddingVertical:2 },
  badgeText:       { fontSize:10, fontWeight:"700" },
  badge_notif:     { position:"absolute", top:-4, right:-6, backgroundColor:C.red, borderRadius:99, minWidth:16, height:16, alignItems:"center", justifyContent:"center", paddingHorizontal:3 },
  badge_notif_text:{ color:"#fff", fontSize:8, fontWeight:"700" },
  // Toast
  toastContainer:  { position:"absolute", top:100, right:16, zIndex:9999, rowGap: 8, columnGap: 8 },
  toast:           { borderWidth:1, borderRadius:10, padding:12, minWidth:220, maxWidth:300, shadowColor:"#000", shadowOpacity:0.3, shadowRadius:8, elevation:8 },
  toastText:       { fontSize:13, fontWeight:"600" },
  // Form
  fieldLabel:      { color:C.muted, fontSize:10, fontWeight:"700", letterSpacing:1.2, marginBottom:6, textTransform:"uppercase" },
  input:           { backgroundColor:"#1e293b", borderWidth:1, borderColor:"#334155", borderRadius:10, paddingHorizontal:14, paddingVertical:12, color:C.text, fontSize:14, marginBottom:2 },
  submitBtn:       { borderRadius:12, padding:14, alignItems:"center", marginTop:8, marginBottom:24 },
  submitBtnText:   { fontSize:15, fontWeight:"700" },
  backBtn:         { color:C.blue, fontSize:13, fontWeight:"700", marginBottom:14 },
  // Bar chart
  barRow:          { flexDirection:"row", justifyContent:"space-between", marginBottom:4 },
  barLabel:        { color:"#cbd5e1", fontSize:12, flex:1 },
  barCount:        { fontSize:12, fontWeight:"700" },
  barTrack:        { backgroundColor:"#0f172a", borderRadius:99, height:7, overflow:"hidden" },
  barFill:         { height:"100%", borderRadius:99 },
  // Empty state
  emptyState:      { alignItems:"center", paddingVertical:48, rowGap: 10, columnGap: 10 },
  emptyIcon:       { fontSize:42 },
  emptyText:       { color:C.muted, fontSize:13 },
  // Tab bar
  tabBar:          { flexDirection:"row", backgroundColor:C.card, borderTopWidth:1, borderTopColor:C.border, paddingVertical:6 },
  tabItem:         { flex:1, alignItems:"center", rowGap: 3, columnGap: 3, paddingVertical:4 },
  tabItemActive:   { borderTopWidth:2, borderTopColor:C.blue },
  tabLabel:        { color:C.muted, fontSize:9, fontWeight:"700", letterSpacing:0.5 },
  tabLabelActive:  { color:C.blue },
});
