import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'

import FaceLabeling from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = asset.id

const noop = () => () => {}

jest.mock('../helpers')

describe('<FaceLabeling />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<FaceLabeling />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render selected asset with no data', () => {
    require('swr').__setMockUseSWRResponse({})

    require('next/router').__setUseRouter({
      query: { id: ASSET_ID, projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<FaceLabeling />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render selected asset with predictions', () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        ...asset,
        predictions: [
          {
            score: 0.999,
            bbox: [0.38, 0.368, 0.484, 0.584],
            label: 'face1',
            simhash: 'MNONPMMKPLRLONLJMRLNM',
            b64_image: 'data:image/png;base64',
          },
        ],
      },
    })

    require('next/router').__setUseRouter({
      query: { id: ASSET_ID, projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<FaceLabeling />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ id: 'MNONPMMKPLRLONLJMRLNM' })
        .props.onChange({ value: 'Jane' })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Cancel' })
        .props.onClick({ preventDefault: noop })
    })

    act(() => {
      component.root
        .findByProps({ id: 'MNONPMMKPLRLONLJMRLNM' })
        .props.onChange({ value: 'Jane' })
    })

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
