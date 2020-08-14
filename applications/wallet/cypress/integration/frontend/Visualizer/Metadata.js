describe('Visualizer', function () {
  describe('Metadata', function () {
    beforeEach(() => {
      /**
       * zooming in displays fewer assets
       * which causes less stress on the network
       * and triggers fewer errors
       */
      localStorage.setItem(
        'Assets',
        JSON.stringify({ columnCount: 1, isMin: false, isMax: true }),
      )
    })

    it('are pretty or raw', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('button[aria-label*="Asset Metadata"]').click()

      cy.contains('Select an asset to view its metadata.')

      cy.get('a[aria-label*="Select asset"]')
        .first()
        // element tends to rerender/detach, which confuses cypress
        // so we pass `{ force: true }` to keep going
        // cf https://github.com/cypress-io/cypress/issues/7306
        .click({ force: true })
        .then((response) => {
          const { href } = response[0]
          cy.url().should('eq', href)
        })

      cy.url().then((url) => {
        const [, assetId] = url.match(/\?assetId=(.*)$/)

        cy.contains(assetId)
      })

      cy.contains('raw json').click()

      cy.contains('"dataSourceId":')

      cy.contains('pretty').click()

      cy.contains('System').click()

      cy.contains('Data Source ID')
    })
  })
})
