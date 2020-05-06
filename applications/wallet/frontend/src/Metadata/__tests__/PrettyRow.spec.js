import TestRenderer from 'react-test-renderer'

import pdfAsset from '../../Asset/__mocks__/pdfAsset'
import bboxAsset from '../../Asset/__mocks__/bboxAsset'

import MetadataPrettyRow from '../PrettyRow'

describe('<MetadataPrettyRow />', () => {
  it('should render documents', () => {
    const pdfContent = pdfAsset.metadata.media.content

    const component = TestRenderer.create(
      <MetadataPrettyRow name="content" value={pdfContent} path="media" />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render when value is of type object', () => {
    const objectValue = bboxAsset.metadata.files[0].attrs

    const component = TestRenderer.create(
      <MetadataPrettyRow name="files" value={objectValue} path="files" />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
