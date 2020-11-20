import TestRenderer from 'react-test-renderer'

import Resizeable, { noop } from '..'

describe('<Resizeable />', () => {
  it('should render properly initially closed', () => {
    const component = TestRenderer.create(
      <Resizeable
        storageName="CursorRightWithMessage"
        minSize={400}
        openToThe="right"
        header={() => <div>Header</div>}
        isInitiallyOpen={false}
      >
        {() => <div>Body</div>}
      </Resizeable>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly initially open', () => {
    const component = TestRenderer.create(
      <Resizeable
        storageName="CursorLeftWithMessage"
        minSize={400}
        openToThe="left"
        header={() => <div>Header</div>}
        isInitiallyOpen
      >
        {() => <div>Body</div>}
      </Resizeable>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render with node children', () => {
    const component = TestRenderer.create(
      <Resizeable
        storageName="CursorTopWithMessage"
        minSize={400}
        openToThe="top"
        isInitiallyOpen
      >
        <div>Body</div>
      </Resizeable>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with function children', () => {
    const component = TestRenderer.create(
      <Resizeable
        storageName="CursorBottomWithMessage"
        minSize={400}
        openToThe="bottom"
        header={() => <div>Header</div>}
        isInitiallyOpen
      >
        {() => <div>Body</div>}
      </Resizeable>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
