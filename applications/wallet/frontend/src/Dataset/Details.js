import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import { spacing, constants } from '../Styles'

import ItemTitle from '../Item/Title'
import ItemList from '../Item/List'
import Menu from '../Menu'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { decamelize } from '../Text/helpers'
import { useLabelTool } from '../AssetLabeling/helpers'

import KebabSvg from '../Icons/kebab.svg'

import DatasetDeleteModal from './DeleteModal'

const DatasetDetails = ({ projectId, datasetId }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)
  const [, setDatasetFields] = useLabelTool({ projectId })

  const {
    data: { name, type, description },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/${datasetId}/`)

  return (
    <div>
      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
        }}
      >
        <ItemTitle type="Dataset" name={name} />

        <Menu
          open="bottom-left"
          button={({ onBlur, onClick }) => (
            <Button
              aria-label="Toggle Actions Menu"
              variant={BUTTON_VARIANTS.SECONDARY}
              onBlur={onBlur}
              onClick={onClick}
              style={{ padding: spacing.moderate, marginBottom: spacing.small }}
            >
              <KebabSvg height={constants.icons.regular} />
            </Button>
          )}
        >
          {({ onBlur, onClick }) => (
            <div>
              <ul>
                <li>
                  <Button
                    variant={BUTTON_VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={async () => {
                      onClick()
                      setDeleteModalOpen(true)
                    }}
                  >
                    Delete Dataset
                  </Button>
                </li>
              </ul>
            </div>
          )}
        </Menu>

        <DatasetDeleteModal
          projectId={projectId}
          datasetId={datasetId}
          name={name}
          isDeleteModalOpen={isDeleteModalOpen}
          setDeleteModalOpen={setDeleteModalOpen}
          setDatasetFields={setDatasetFields}
        />
      </div>

      <ItemList
        attributes={[
          ['Dataset Type', decamelize({ word: type })],
          ['Description', description],
        ]}
      />
    </div>
  )
}

DatasetDetails.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
}

export default DatasetDetails
