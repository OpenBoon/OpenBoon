import TestRenderer, { act } from 'react-test-renderer'

import FiltersCopyQuery from '../CopyQuery'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<FiltersCopyQuery />', () => {
  it('should copy properly', async () => {
    const mockCopyFn = jest.fn()

    window.navigator.clipboard.writeText = mockCopyFn

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        query: btoa(
          JSON.stringify([
            { type: 'textContent', attribute: '', values: { query: 'Cat' } },
          ]),
        ),
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          query: {
            bool: { must: [{ simple_query_string: { query: 'cat' } }] },
          },
        },
      },
    })

    const component = TestRenderer.create(<FiltersCopyQuery />)

    expect(component.toJSON()).toMatchSnapshot()

    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'Copy Search Query to Clipboard' })
        .props.onClick()
    })

    expect(mockCopyFn).toHaveBeenCalledWith(
      '{"query":{"bool":{"must":[{"simple_query_string":{"query":"cat"}}]}}}',
    )
  })
})
