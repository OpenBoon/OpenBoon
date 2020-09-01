/**
 * getCookies()
 */
const getCookies = () => {
  return Object.fromEntries(
    document.cookie.split(/; */).map((c) => {
      const [key, ...v] = c.split('=')
      return [key, decodeURIComponent(v.join('='))]
    }),
  )
}

/**
 * Extends the `cy.request()` function to have the headers needed to use the
 * Wallet REST API. For convenience it allows you to set an array of acceptable
 * status codes for the response as well.
 */
Cypress.Commands.add(
  'fetch',
  ({ url, method = 'GET', body, okStatusCodes = [200, 201, 207], ...rest }) => {
    cy.request({
      url,
      method,
      body,
      failOnStatusCode: false,
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': getCookies().csrftoken,
        Referer: Cypress.config('baseUrl'),
      },
      ...rest,
    }).then(({ status }) => {
      if (!okStatusCodes.includes(status)) {
        throw new Error(
          `Expected ${okStatusCodes} status code, but got ${status} instead`,
        )
      }
    })
  },
)
