module.exports = (on, config) => {
  if (config.hasOwnProperty('env') && config.env.hasOwnProperty('BASE_URL')) {
    config.baseUrl = config.env.BASE_URL
  }
  return config
}
