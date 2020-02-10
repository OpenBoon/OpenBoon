import TestRenderer from 'react-test-renderer'
import Modal from '..'

const noop = () => () => {}

describe('<Modal />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()
    Object.defineProperty(document, 'getElementById', { value: mockFn })

    const component = TestRenderer.create(
      <Modal
        title="Delete API Key"
        message="Deleting this key cannot be undone."
        action="Delete Permanently"
        onCancel={noop}
        onConfirm={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    component.root
      .findByProps({ verticallyCenter: true })
      .props.getApplicationNode()

    expect(mockFn).toHaveBeenCalledWith('__next')
  })
})
