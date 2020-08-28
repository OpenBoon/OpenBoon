module.exports = (on, config) => {
  on('before:browser:launch', (browser = {}, launchOptions) => {
    if (browser.name === 'chrome') {
      launchOptions.args.push('--disable-dev-shm-usage')
      return launchOptions
    }
  })

  if (config.hasOwnProperty('env') && config.env.hasOwnProperty('BASE_URL')) {
    config.baseUrl = config.env.BASE_URL
  }

  return config
}
