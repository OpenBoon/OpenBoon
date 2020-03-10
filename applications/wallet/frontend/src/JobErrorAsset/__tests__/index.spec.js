import TestRenderer from 'react-test-renderer'

import JobErrorAsset from '../index'
import assets from '../../Assets/__mocks__/assets'

describe('<JobErrorAsset />', () => {
  it('should render properly', () => {
    const asset = assets.results[0]

    const component = TestRenderer.create(<JobErrorAsset asset={asset} />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
