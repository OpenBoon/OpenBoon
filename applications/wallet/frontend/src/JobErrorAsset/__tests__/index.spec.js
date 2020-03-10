import TestRenderer from 'react-test-renderer'

import JobErrorAsset from '../index'

describe('<JobErrorAsset />', () => {
  it('should render properly', () => {
    // temporary asset
    const asset = {
      metadata: { source: { url: 'some-url', filename: 'some-filename' } },
    }

    const component = TestRenderer.create(<JobErrorAsset asset={asset} />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
