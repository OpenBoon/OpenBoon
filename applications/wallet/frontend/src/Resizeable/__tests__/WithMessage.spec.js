import TestRenderer from 'react-test-renderer'

import ResizeableWithMessage from '../WithMessage'

describe('<Resizeable />', () => {
  it('should render properly initially closed', () => {
    const component = TestRenderer.create(
      <ResizeableWithMessage
        storageName="CursorLeftWithMessage"
        minSize={400}
        openToThe="left"
        header={() => <div>Header</div>}
        isInitiallyOpen={false}
      >
        {() => <div>Body</div>}
      </ResizeableWithMessage>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with the cursor left', () => {
    const component = TestRenderer.create(
      <ResizeableWithMessage
        storageName="CursorLeftWithMessage"
        minSize={400}
        openToThe="left"
        header={() => <div>Header</div>}
        isInitiallyOpen
      >
        {() => <div>Body</div>}
      </ResizeableWithMessage>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
