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
        .findByType('button')
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
