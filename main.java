import java.util.ArrayList;
import java.util.Scanner;

// Interface for scheduling appointments
interface Schedulable {
    void scheduleAppointment(Appointment appointment);
    void cancelAppointment(String appointmentId);
    void displayAppointments();
}

// Custom exception for booking errors
class BookingException extends Exception {
    BookingException(String message) {
        super(message);
    }
}

// Abstract class for a general person
abstract class Person {
    String id;
    String name;
    int age;

    // Constructor to initialize values
    Person(String id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    // Abstract method to display details
    abstract void displayDetails();
}

// Appointment class to store appointment information
class Appointment {
    String appointmentId;
    String patientName;
    String doctorName;
    String date;
    String time;

    // Constructor to initialize values
    Appointment(String appointmentId, String patientName, String doctorName, String date, String time) {
        this.appointmentId = appointmentId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.date = date;
        this.time = time;
    }

    // Method to print appointment details
    void displayDetails() {
        System.out.println("Appointment ID : " + appointmentId);
        System.out.println("Patient Name   : " + patientName);
        System.out.println("Doctor Name    : " + doctorName);
        System.out.println("Date           : " + date);
        System.out.println("Time           : " + time);
        System.out.println("___________________________________");
    }

    // Override toString method
    public String toString() {
        return "ID: " + appointmentId + " | Patient: " + patientName + " | Doctor: " + doctorName + " | Date: " + date + " | Time: " + time;
    }
}

// Patient class extends Person
class Patient extends Person {
    String disease;
    String assignedDoctorId;

    // Constructor to initialize values
    Patient(String id, String name, int age, String disease) {
        super(id, name, age);
        this.disease = disease;
        this.assignedDoctorId = "Not Assigned";
    }

    // Method to assign a doctor
    void assignDoctor(String doctorId) {
        this.assignedDoctorId = doctorId;
    }

    // Method to display patient details
    void displayDetails() {
        System.out.println("Patient ID     : " + id);
        System.out.println("Patient Name   : " + name);
        System.out.println("Age            : " + age);
        System.out.println("Disease        : " + disease);
        System.out.println("Assigned Doctor: " + assignedDoctorId);
        System.out.println("___________________________________");
    }
}

// Doctor class extends Person and implements Schedulable
class Doctor extends Person implements Schedulable {
    String specialization;
    ArrayList<Appointment> appointments;

    // Constructor to initialize values
    Doctor(String id, String name, int age, String specialization) {
        super(id, name, age);
        this.specialization = specialization;
        this.appointments = new ArrayList<Appointment>();
    }

    // Method to schedule an appointment
    public void scheduleAppointment(Appointment appointment) {
        try {
            // Check for double booking
            for (int i = 0; i < appointments.size(); i++) {
                Appointment a = appointments.get(i);
                if (a.date.equals(appointment.date) && a.time.equals(appointment.time)) {
                    throw new BookingException("Doctor " + name + " is already booked on " + appointment.date + " at " + appointment.time);
                }
            }
            // No conflict, add appointment
            appointments.add(appointment);
            System.out.println("Appointment scheduled successfully!");

        } catch (BookingException e) {
            System.out.println("Booking Error: " + e.getMessage());
        }
    }

    // Method to cancel an appointment
    public void cancelAppointment(String appointmentId) {
        boolean found = false;
        for (int i = 0; i < appointments.size(); i++) {
            if (appointments.get(i).appointmentId.equals(appointmentId)) {
                appointments.remove(i);
                System.out.println("Appointment " + appointmentId + " cancelled successfully.");
                found = true;
                break;
            }
        }
        if (found == false) {
            System.out.println("Appointment ID " + appointmentId + " not found.");
        }
    }

    // Method to display all appointments
    public void displayAppointments() {
        if (appointments.size() == 0) {
            System.out.println("No appointments for Dr. " + name);
        } else {
            System.out.println("Appointments for Dr. " + name + ":");
            for (int i = 0; i < appointments.size(); i++) {
                appointments.get(i).displayDetails();
            }
        }
    }

    // Method to display doctor details
    void displayDetails() {
        System.out.println("Doctor ID      : " + id);
        System.out.println("Doctor Name    : Dr. " + name);
        System.out.println("Age            : " + age);
        System.out.println("Specialization : " + specialization);
        System.out.println("___________________________________");
    }
}

// Hospital class to manage patients and doctors
class Hospital {
    String hospitalName;
    ArrayList<Patient> patients;
    ArrayList<Doctor> doctors;

    // Constructor to initialize values
    Hospital(String hospitalName) {
        this.hospitalName = hospitalName;
        this.patients = new ArrayList<Patient>();
        this.doctors = new ArrayList<Doctor>();
    }

    // Method to register a patient
    void registerPatient(Patient patient) {
        patients.add(patient);
        System.out.println("Patient " + patient.name + " registered successfully.");
    }

    // Method to add a doctor
    void addDoctor(Doctor doctor) {
        doctors.add(doctor);
        System.out.println("Doctor " + doctor.name + " added successfully.");
    }

    // Method to find a patient by ID
    Patient findPatient(String patientId) {
        for (int i = 0; i < patients.size(); i++) {
            if (patients.get(i).id.equals(patientId)) {
                return patients.get(i);
            }
        }
        return null;
    }

    // Method to find a doctor by ID
    Doctor findDoctor(String doctorId) {
        for (int i = 0; i < doctors.size(); i++) {
            if (doctors.get(i).id.equals(doctorId)) {
                return doctors.get(i);
            }
        }
        return null;
    }

    // Method to assign a doctor to a patient
    void assignDoctorToPatient(String patientId, String doctorId) {
        Patient patient = findPatient(patientId);
        Doctor doctor = findDoctor(doctorId);

        if (patient == null) {
            System.out.println("Patient ID " + patientId + " not found.");
            return;
        }
        if (doctor == null) {
            System.out.println("Doctor ID " + doctorId + " not found.");
            return;
        }

        patient.assignDoctor(doctorId);
        System.out.println("Dr. " + doctor.name + " assigned to " + patient.name + " successfully.");
    }

    // Method to book an appointment
    void bookAppointment(String appointmentId, String patientId, String doctorId, String date, String time) {
        Patient patient = findPatient(patientId);
        Doctor doctor = findDoctor(doctorId);

        if (patient == null) {
            System.out.println("Patient ID " + patientId + " not found.");
            return;
        }
        if (doctor == null) {
            System.out.println("Doctor ID " + doctorId + " not found.");
            return;
        }

        Appointment appointment = new Appointment(appointmentId, patient.name, doctor.name, date, time);
        doctor.scheduleAppointment(appointment);
    }

    // Method to display all patients
    void displayAllPatients() {
        if (patients.size() == 0) {
            System.out.println("No patients registered.");
            return;
        }
        System.out.println("========= ALL PATIENTS =========");
        for (int i = 0; i < patients.size(); i++) {
            patients.get(i).displayDetails();
        }
    }

    // Method to display all doctors
    void displayAllDoctors() {
        if (doctors.size() == 0) {
            System.out.println("No doctors available.");
            return;
        }
        System.out.println("========= ALL DOCTORS ==========");
        for (int i = 0; i < doctors.size(); i++) {
            doctors.get(i).displayDetails();
        }
    }
}

// Main class - runs the program
public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        Hospital hospital = new Hospital("Mount Kigali General Hospital");

        System.out.println("   WELCOME TO " + hospital.hospitalName.toUpperCase());
        System.out.println("______________________________");

        int choice = 0;

        while (choice != 7) {

            System.out.println("\n======== MAIN MENU ========");
            System.out.println("1. Register Patient");
            System.out.println("2. Add Doctor");
            System.out.println("3. Assign Doctor to Patient");
            System.out.println("4. Book Appointment");
            System.out.println("5. View All Patients");
            System.out.println("6. View All Doctors and Appointments");
            System.out.println("7. Exit");
            System.out.print("Enter your choice: ");

            choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 1) {
                System.out.println("\n--- Register New Patient ---");
                System.out.print("Enter Patient ID   : ");
                String id = scanner.nextLine();
                System.out.print("Enter Patient Name : ");
                String name = scanner.nextLine();
                System.out.print("Enter Age          : ");
                int age = scanner.nextInt();
                scanner.nextLine();
                System.out.print("Enter Disease      : ");
                String disease = scanner.nextLine();

                Patient patient = new Patient(id, name, age, disease);
                hospital.registerPatient(patient);

            } else if (choice == 2) {
                System.out.println("\n--- Add New Doctor ---");
                System.out.print("Enter Doctor ID      : ");
                String id = scanner.nextLine();
                System.out.print("Enter Doctor Name    : ");
                String name = scanner.nextLine();
                System.out.print("Enter Age            : ");
                int age = scanner.nextInt();
                scanner.nextLine();
                System.out.print("Enter Specialization : ");
                String spec = scanner.nextLine();

                Doctor doctor = new Doctor(id, name, age, spec);
                hospital.addDoctor(doctor);

            } else if (choice == 3) {
                System.out.println("\n--- Assign Doctor to Patient ---");
                System.out.print("Enter Patient ID : ");
                String patientId = scanner.nextLine();
                System.out.print("Enter Doctor ID  : ");
                String doctorId = scanner.nextLine();

                hospital.assignDoctorToPatient(patientId, doctorId);

            } else if (choice == 4) {
                System.out.println("\n--- Book Appointment ---");
                System.out.print("Enter Appointment ID         : ");
                String apptId = scanner.nextLine();
                System.out.print("Enter Patient ID             : ");
                String patientId = scanner.nextLine();
                System.out.print("Enter Doctor ID              : ");
                String doctorId = scanner.nextLine();
                System.out.print("Enter Date (e.g. 2026-03-13) : ");
                String date = scanner.nextLine();
                System.out.print("Enter Time (e.g. 10:00 AM)   : ");
                String time = scanner.nextLine();

                hospital.bookAppointment(apptId, patientId, doctorId, date, time);

            } else if (choice == 5) {
                hospital.displayAllPatients();

            } else if (choice == 6) {
                hospital.displayAllDoctors();
                System.out.println("\n======= APPOINTMENTS =======");
                for (int i = 0; i < hospital.doctors.size(); i++) {
                    hospital.doctors.get(i).displayAppointments();
                }

            } else if (choice == 7) {
                System.out.println("\nGoodbye! Thank you for using the Hospital Management System.");

            } else {
                System.out.println("Invalid choice. Please enter a number from 1 to 7.");
            }
        }

        scanner.close();
    }
}