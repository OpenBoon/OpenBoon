import TestRenderer from 'react-test-renderer'

import ProgressBar from '..'

describe('<ProgressBar />', () => {
  it('should render properly', () => {
    const status = { isGenerating: true, isCanceled: false, canceledBy: '' }
    const component = TestRenderer.create(<ProgressBar status={status} />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
