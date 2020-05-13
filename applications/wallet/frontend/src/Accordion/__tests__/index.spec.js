import TestRenderer, { act } from 'react-test-renderer'

import Accordion, { VARIANTS } from '..'

const noop = () => () => {}

describe('<Accordion />', () => {
  Object.keys(VARIANTS).forEach((variant) => {
    it(`should render properly for ${variant}`, () => {
      const component = TestRenderer.create(
        <Accordion
          variant={variant}
          title="Hi"
          cacheKey={`cacheKey.${variant}`}
          isInitiallyOpen={false}
        >
          Hello
        </Accordion>,
      )

      expect(component.toJSON()).toMatchSnapshot()

      act(() => {
        component.root
          .findByProps({ 'aria-label': 'Expand Section' })
          .props.onClick({ preventDefault: noop })
      })

      expect(
        component.root.findByProps({ 'aria-label': 'Collapse Section' }),
      ).toBeTruthy()
    })
  })
})
