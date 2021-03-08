describe('Visualizer', function () {
  describe('Assets', function () {
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

    it('can be selected', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.selectFirstAsset()

      cy.url().should('match', /(.*)\/visualizer\?assetId=(.*)/)
    })

    it('can navigate to the asset details page', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('a[aria-label*="Asset details"]')
        .first()
        // element tends to rerender/detach, which confuses cypress
        // so we pass `{ force: true }` to keep going
        // cf https://github.com/cypress-io/cypress/issues/7306
        .click({ force: true })
        .then((response) => {
          const { href } = response[0]
          cy.url().should('eq', href)
        })

      cy.url().should('match', /(.*)\/visualizer\/(.*)/)
    })

    it('can find similar images', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('button[aria-label*="Find similar images"]').first().click()

      cy.url().should('match', /(.*)\/visualizer\?query=(.*)/)

      cy.get('button[aria-label*="Filters"]').click()

      cy.contains('analysis.boonai-image-similarity')

      cy.selectFirstAsset()

      cy.contains('Similarity Range: 0.75')

      cy.get('button[role="slider"]')
        .trigger('mousedown', { which: 1 })
        .trigger('mousemove', { which: 1, pageX: 0, pageY: 0 })
        .trigger('mouseup')

      cy.contains('Similarity Range: 0.01')
    })
  })
})
