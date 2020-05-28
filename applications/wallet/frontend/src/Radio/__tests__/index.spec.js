import TestRenderer from 'react-test-renderer'

import Radio from '..'

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
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
