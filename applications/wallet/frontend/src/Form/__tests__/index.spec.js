import TestRenderer from 'react-test-renderer'

import permissions from '../../Permissions/__mocks__/permissions'

import Form from '..'

describe('<Form />', () => {
  it('should not POST the form', () => {
    const mockFn = jest.fn()

    require('swr').__setMockUseSWRResponse({
      data: permissions,
    })

    const component = TestRenderer.create(<Form>Yo</Form>)

    component.root
      .findByProps({ method: 'post' })
      .props.onSubmit({ preventDefault: mockFn })

    expect(mockFn).toHaveBeenCalled()
  })
})
