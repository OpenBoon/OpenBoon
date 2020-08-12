import TestRenderer, { act } from 'react-test-renderer'

import Accordion, { VARIANTS } from '..'

describe('<Accordion />', () => {
  Object.keys(VARIANTS).forEach((variant) => {
    it(`should render properly for ${variant}`, () => {
      const component = TestRenderer.create(
        <Accordion
          variant={variant}
          title="Hi"
          cacheKey={`cacheKey.${variant}`}
          isInitiallyOpen={false}
          isResizeable={variant === 'FILTER'}
        >
          Hello
        </Accordion>,
      )

      expect(component.root.findByType('details').props.open).toBeFalsy()

      act(() => {
        component.root
          .findByType('details')
          .props.onToggle({ target: { open: true } })
      })

      expect(component.root.findByType('details').props.open).toBeTruthy()
    })
  })
})
