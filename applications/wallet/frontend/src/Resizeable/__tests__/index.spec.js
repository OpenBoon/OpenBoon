import TestRenderer from 'react-test-renderer'

import Resizeable, { noop } from '..'

describe('<Resizeable />', () => {
  it('should render properly with the cursor left', () => {
    const component = TestRenderer.create(
      <Resizeable
        minExpandedSize={400}
        minCollapsedSize={0}
        storageName="yoga-div"
        openToThe="left"
        onMouseUp={noop}
      >
        {() => 'Yoga div'}
      </Resizeable>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with the cursor right', () => {
    const component = TestRenderer.create(
      <Resizeable
        minExpandedSize={400}
        minCollapsedSize={300}
        storageName="yoga-div"
        openToThe="right"
        onMouseUp={noop}
      >
        {() => 'Yoga div'}
      </Resizeable>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with the cursor top', () => {
    const component = TestRenderer.create(
      <Resizeable
        minExpandedSize={400}
        minCollapsedSize={300}
        storageName="yoga-div"
        openToThe="top"
        onMouseUp={noop}
      >
        {() => 'Yoga div'}
      </Resizeable>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
