<template>
  <div class="register-container">
    <h2>Création de Profil</h2>
    <form @submit.prevent="handleRegister">
      <div class="form-group">
        <label for="email">Email:</label>
        <input type="email" v-model="email" required placeholder="Entrez votre email">
      </div>
      <div class="form-group">
        <label for="password">Mot de passe:</label>
        <input type="password" v-model="password" required placeholder="Entrez votre mot de passe">
      </div>
      <button type="submit" :disabled="loading">
        {{ loading ? 'Inscription en cours...' : 'S\'inscrire' }}
      </button>
    </form>
    <p v-if="message" :class="{ 'error': isError, 'success': !isError }">{{ message }}</p>
  </div>
</template>

<script>
export default {
  name: 'RegisterProfile',
  data() {
    return {
      email: '',
      password: '',
      loading: false,
      message: '',
      isError: false
    }
  },
  methods: {
    async handleRegister() {
      this.loading = true;
      this.message = '';
      this.isError = false;

      try {
        const response = await fetch('http://localhost:8080/api/users/register', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            email: this.email,
            passwordHash: this.password // On envoie le password en tant que passwordHash comme convenu pour le backend
          })
        });

        const data = await response.json();

        if (response.ok) {
          this.message = 'Profil créé avec succès ! (Sync Firebase OK)';
          this.email = '';
          this.password = '';
        } else {
          this.isError = true;
          this.message = data.message || 'Erreur lors de la création.';
        }
      } catch (error) {
        this.isError = true;
        this.message = 'Erreur de connexion au serveur backend.';
      } finally {
        this.loading = false;
      }
    }
  }
}
</script>

<style scoped>
.register-container {
  max-width: 400px;
  margin: 50px auto;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
.form-group {
  margin-bottom: 15px;
  text-align: left;
}
label {
  display: block;
  margin-bottom: 5px;
}
input {
  width: 100%;
  padding: 8px;
  box-sizing: border-box;
}
button {
  width: 100%;
  padding: 10px;
  background-color: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
button:disabled {
  background-color: #a5d6a7;
}
.error {
  color: red;
}
.success {
  color: green;
}
</style>
