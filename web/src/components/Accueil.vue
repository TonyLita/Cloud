<template>
  <div class="home-container">
    <h1>Bienvenue sur votre espace</h1>
    
    <div v-if="user" class="content">
      <div class="info-card">
        <h3>Votre Profil</h3>
        <p>Email : <strong>{{ user.email }}</strong></p>
        <p>Rôle : <span class="role-badge">{{ user.role.label }}</span></p>
        
        <div v-if="!editing && user.role.id === 2">
          <button @click="startEdit(user)" class="btn-edit">Modifier mon profil</button>
          <button @click="handleDelete(user.id)" class="btn-delete">Supprimer mon compte</button>
        </div>
      </div>

      <!-- Affichage Admin : Liste des utilisateurs -->
      <div v-if="user.role.id === 1" class="admin-section">
        <h3>Gestion des utilisateurs (Admin)</h3>
        
        <div class="admin-grid">
          <div class="config-card">
            <h4>Configuration Session</h4>
            <div class="form-inline">
              <label>Durée (min) :</label>
              <input type="number" v-model="sessionTimeout" min="1" max="1440">
              <button @click="updateSessionTimeout" class="btn-save btn-sm">Mettre à jour</button>
            </div>
            <p class="help-text">Synchronisé avec SQL et Firebase</p>
          </div>

          <div class="config-card blocked-card">
            <h4>Utilisateurs Bloqués (userbloc)</h4>
            <ul v-if="blockedList.length > 0" class="blocked-list">
              <li v-for="b in blockedList" :key="b.id">
                <span>{{ b.user.email }}</span>
                <button @click="unlockUser(b.user.id)" class="btn-unlock btn-sm">Débloquer</button>
              </li>
            </ul>
            <p v-else class="help-text">Aucun utilisateur bloqué.</p>
          </div>
        </div>

        <table class="user-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Email</th>
              <th>Rôle</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in usersList" :key="u.id">
              <td>{{ u.id }}</td>
              <td>{{ u.email }}</td>
              <td>{{ u.role.label }}</td>
              <td>
                <button @click="startEdit(u)" class="btn-sm btn-edit">Modifier</button>
                <button @click="handleDelete(u.id)" class="btn-sm btn-delete">Supprimer</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Formulaire de modification (Modal simple) -->
      <div v-if="editing" class="edit-modal">
        <div class="modal-content">
          <h3>Modifier utilisateur</h3>
          <form @submit.prevent="submitUpdate">
            <div class="form-group">
              <label>Nouvel Email :</label>
              <input type="email" v-model="formUser.email" required>
            </div>
            <div class="form-group">
              <label>Nouveau Mot de passe (optionnel) :</label>
              <input type="password" v-model="formUser.password" placeholder="Laissez vide pour garder l'actuel">
            </div>
            <div class="modal-actions">
              <button type="submit" class="btn-save">Enregistrer</button>
              <button type="button" @click="editing = false" class="btn-cancel">Annuler</button>
            </div>
          </form>
        </div>
      </div>

      <button @click="handleLogout" class="btn-logout">Déconnexion</button>
    </div>
    <div v-else>
      <p>Chargement...</p>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'AccueilUser',
  data() {
    return {
      user: null,
      usersList: [],
      blockedList: [],
      sessionTimeout: 30,
      editing: false,
      formUser: { id: null, email: '', password: '' }
    }
  },
  async created() {
    await this.fetchStatus();
    this.setupAutoLogout();
  },
  methods: {
    setupAutoLogout() {
      // Vérification périodique de la session toutes les minutes
      setInterval(async () => {
        try {
          await axios.get('http://localhost:8080/api/users/me', { withCredentials: true });
        } catch (err) {
          // Si 401 ou erreur, la session a expiré côté serveur
          this.handleLogout();
        }
      }, 60000); 
    },
    async fetchStatus() {
      try {
        const response = await axios.get('http://localhost:8080/api/users/me', { withCredentials: true });
        this.user = response.data;
        if (this.user.role.id === 1) {
          this.fetchUsersList();
          this.fetchBlockedUsers();
          this.fetchSessionTimeout();
        }
      } catch (err) {
        this.$router.push('/login');
      }
    },
    async fetchUsersList() {
      const res = await axios.get('http://localhost:8080/api/users', { withCredentials: true });
      this.usersList = res.data;
    },
    async fetchBlockedUsers() {
      try {
        const res = await axios.get('http://localhost:8080/api/users/blocked', { withCredentials: true });
        this.blockedList = res.data;
      } catch (err) {
        console.error("Erreur chargement bloqués", err);
      }
    },
    async unlockUser(userId) {
      try {
        await axios.put(`http://localhost:8080/api/users/${userId}/unlock`, {}, { withCredentials: true });
        await this.fetchBlockedUsers();
        await this.fetchUsersList();
        alert('Utilisateur débloqué !');
      } catch (err) {
        alert('Erreur lors du déblocage');
      }
    },
    async fetchSessionTimeout() {
      try {
        const res = await axios.get('http://localhost:8080/api/users/config/timeout', { withCredentials: true });
        this.sessionTimeout = res.data;
      } catch (err) {
        console.error("Erreur lors de la récupération du timeout", err);
      }
    },
    async updateSessionTimeout() {
      try {
        await axios.put('http://localhost:8080/api/users/config/timeout', { minutes: this.sessionTimeout }, { withCredentials: true });
        alert('Durée de session mise à jour et synchronisée avec Firebase !');
      } catch (err) {
        alert('Erreur lors de la mise à jour du timeout');
      }
    },
    startEdit(u) {
      this.editing = true;
      this.formUser = { id: u.id, email: u.email, password: '' };
    },
    async submitUpdate() {
      try {
        // On n'envoie que les données brutes, pas l'objet complet pour éviter les erreurs de désérialisation JPA
        const payload = {
          email: this.formUser.email,
          passwordHash: this.formUser.password 
        };
        
        await axios.put(`http://localhost:8080/api/users/${this.formUser.id}`, payload, { withCredentials: true });
        
        this.editing = false;
        await this.fetchStatus(); // Rafraîchir les infos
        alert('Modification réussie');
      } catch (err) {
        console.error(err);
        alert('Erreur 400 : Vérifiez que l\'email n\'est pas déjà utilisé ou que les champs sont valides.');
      }
    },
    async handleDelete(id) {
      if (confirm('Êtes-vous sûr de vouloir supprimer ce compte ?')) {
        try {
          await axios.delete(`http://localhost:8080/api/users/${id}`, { withCredentials: true });
          if (id === this.user.id) {
            this.$router.push('/login');
          } else {
            this.fetchUsersList();
          }
        } catch (err) {
          alert('Erreur lors de la suppression');
        }
      }
    },
    async handleLogout() {
      await axios.post('http://localhost:8080/api/users/logout', {}, { withCredentials: true });
      this.$router.push('/login');
    }
  }
}
</script>

<style scoped>
.home-container { padding: 40px; max-width: 800px; margin: auto; }
.info-card { background: #f9f9f9; padding: 20px; border-radius: 8px; margin-bottom: 30px; text-align: left; border-left: 5px solid #3498db; }
.role-badge { background: #3498db; color: white; padding: 4px 10px; border-radius: 12px; font-size: 0.8em; }
.admin-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px; }
.config-card { background: #fff; padding: 15px; border: 1px solid #ddd; border-radius: 8px; border-left: 5px solid #2ecc71; }
.blocked-card { border-left-color: #e74c3c; }
.blocked-list { list-style: none; padding: 0; margin-top: 10px; }
.blocked-list li { display: flex; justify-content: space-between; align-items: center; padding: 5px 0; border-bottom: 1px solid #eee; font-size: 0.9em; }
.btn-unlock { background: #e67e22; color: white; border: none; cursor: pointer; border-radius: 4px; }
.form-inline { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
.form-inline input { padding: 8px; width: 80px; }
.help-text { font-size: 0.8em; color: #7f8c8d; margin-top: 5px; }
.user-table { width: 100%; border-collapse: collapse; margin-top: 20px; }
.user-table th, .user-table td { border: 1px solid #ddd; padding: 12px; text-align: left; }
.user-table th { background-color: #f2f2f2; }
.edit-modal { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; }
.modal-content { background: white; padding: 30px; border-radius: 8px; min-width: 300px; }
.form-group { margin-bottom: 15px; text-align: left; }
.form-group input { width: 100%; padding: 8px; margin-top: 5px; }
.btn-sm { padding: 5px 10px; font-size: 0.8em; margin: 0 5px; }
.btn-edit { background: #f39c12; color: white; border: none; cursor: pointer; border-radius: 4px; }
.btn-delete { background: #e74c3c; color: white; border: none; cursor: pointer; border-radius: 4px; padding: 10px; }
.btn-save { background: #27ae60; color: white; border: none; padding: 10px 20px; cursor: pointer; }
.btn-cancel { background: #95a5a6; color: white; border: none; padding: 10px 20px; cursor: pointer; margin-left:10px; }
.btn-logout { background: #34495e; color: white; border: none; padding: 10px 20px; cursor: pointer; margin-top: 30px; }
</style>
