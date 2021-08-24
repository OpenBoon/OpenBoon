import TestRenderer from 'react-test-renderer'

import Slider, { noop } from '..'

describe('<Slider />', () => {
  it('should render properly when muted', () => {
    const component = TestRenderer.create(
      <Slider
        mode="both"
        step={0.1}
        domain={[0, 100]}
        values={[0, 100]}
        isMuted
        isDisabled={false}
        onUpdate={noop}
        onChange={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when disabled', () => {
    const component = TestRenderer.create(
      <Slider
        mode="both"
        step={0.1}
        domain={[0, 100]}
        values={[0, 100]}
        isMuted
        isDisabled
        onUpdate={noop}
        onChange={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
