<template>
  <main class="public-layout">
    <section class="hero">
      <h1>NekoCafé</h1>
      <p>智慧餐饮预约、猫咪互动与门店运营一体化平台</p>
    </section>
    <router-view />
  </main>
</template>

<style scoped>
.public-layout {
  min-height: 100vh;
  padding: 32px;
}
.hero {
  margin-bottom: 24px;
}
.hero h1 {
  margin: 0;
  font-size: 40px;
}
</style>
