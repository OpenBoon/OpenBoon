import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'

import { colors, constants } from '../Styles'

import ButtonGroup from '../Button/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import DatasetConcepts from '../DatasetConcepts'

const ModelDataset = () => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const {
    data: { datasetId },
  } = useSWR(`/api/v1/projects/${projectId}/models/${modelId}/`)

  if (!datasetId) {
    return (
      <div
        css={{
          color: colors.structure.steel,
          maxWidth: constants.paragraph.maxWidth,
        }}
      >
        <i>
          Datasets are groups of labels added to assets that are used by the
          model for training. When using an uploaded, pre-trained model, only
          the testing labels in the dataset are used.
        </i>

        <ButtonGroup>
          <Link href={`/${projectId}/models/${modelId}/link`} passHref>
            <Button variant={BUTTON_VARIANTS.PRIMARY}>
              Link a Dataset to the Model
            </Button>
          </Link>
        </ButtonGroup>
      </div>
    )
  }

  return (
    <DatasetConcepts
      projectId={projectId}
      datasetId={datasetId}
      actions={false}
    />
  )
}

export default ModelDataset
