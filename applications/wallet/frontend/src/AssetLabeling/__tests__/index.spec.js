import TestRenderer from 'react-test-renderer'

import AssetLabeling from '..'

describe('<AssetLabeling />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<AssetLabeling />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
