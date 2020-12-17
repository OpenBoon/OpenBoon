import TestRenderer from 'react-test-renderer'

import Radio from '..'

const noop = () => () => {}

describe('<Radio />', () => {
  it('should render properly unchecked', () => {
    const component = TestRenderer.create(
      <Radio
        option={{
          label: 'Radio',
          value: 'radio',
          legend: 'radioLegend',
          initialValue: false,
        }}
        onClick={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly checked', () => {
    const component = TestRenderer.create(
      <Radio
        option={{
          label: 'Radio',
          value: 'radio',
          legend: 'radioLegend',
          initialValue: true,
        }}
        onClick={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
