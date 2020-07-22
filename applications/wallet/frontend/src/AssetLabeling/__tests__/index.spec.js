import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'
import models from '../../Models/__mocks__/models'
import project from '../../Project/__mocks__/project'

import AssetLabeling from '..'

const PROJECT_ID = project.id
const ASSET_ID = asset.id

describe('<AssetLabeling />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<AssetLabeling />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render with a selected asset', () => {
    require('swr').__setMockUseSWRResponse({ data: models })
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(<AssetLabeling />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ name: 'models-list' })
        .props.onChange({ target: { value: models.results[0].id } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ id: 'asset-label' })
        .props.onChange({ target: { value: 'Flimflarm' } })

      component.root.findByProps({ type: 'submit' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
