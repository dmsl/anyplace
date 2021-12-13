# Vue Project Setup
Describing how this project was set up using `vue-cli`.

---

# 1. Dependencies:

### 1.1 Install `node.js` and `npm` on your device

The project was set up using node `16.13.1 LTS`.  
Node version `17.x` had issues.

It's better to use `nvm` (Node Package Manager).

Sample commands:  

macOS:  

```bash
brew install nvm
# .. do any other required setup
nvm install 16.13.1
```

### 1.2 Install `vue-cli`:
See [vue-cli](https://cli.vuejs.org/guide/installation.html) for more up-to-date instructions.

Sample commands:  

```bash
npm install -g @vue/cli
```

---

## 2. Create project: `anyplace-vue`:

### 2.1 Initialize project using `vue-cli`:

```bash
vue create anyplace-vue
```

### vue-cli options:
#### Plugins:
- Vue3
- typescript
- node-sass
- babel
- vuex
- eslint
- unit-jest

#### Plugin Options:
- Class-style component syntax: `Y`
- Babel alongside Typescript: `Y`
- History Mode for router: `Y`
- CSS Pre-Processor: `dart-sass`
- linter:
  - ESLint with: `error prevention only`
  - `lint on save`
- Unit Test: `mocha+chai`
- E2E testing: `Cypress`
- config for Babel, ESLint: `dedicated files`


### Tool versions:
- `node.js`: v16.13.1 LTS
- `npm`: 8.2.0
- `@vue/cli`:  4.5.15

```bash
node --version
npm --version
vue --version
```
