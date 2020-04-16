import TestRenderer, { act } from 'react-test-renderer'

import Accordion from '..'

const noop = () => () => {}

describe('<Accordion />', () => {
  it('should render properly with data', () => {
    const component = TestRenderer.create(
      <Accordion title="Hi" isInitiallyOpen={false}>
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
