import { useState } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'
import Router from 'next/router'

import { fetcher, getQueryString } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const DataSourcesMenu = ({ projectId, dataSourceId, name, revalidate }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  return (
    <>
      <Menu open="bottom-left" button={ButtonActions}>
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={async () => {
                    onClick()

                    const { jobId } = await fetcher(
                      `/api/v1/projects/${projectId}/data_sources/${dataSourceId}/scan/`,
                      { method: 'POST' },
                    )

                    const queryString = getQueryString({
                      action: 'scan-datasource-success',
                      jobId,
                    })

                    Router.push(
                      `/[projectId]/data-sources${queryString}`,
                      `/${projectId}/data-sources`,
                    )
                  }}
                >
                  Scan For New Files
                </Button>
              </li>
              <li>
                <Link
                  href="/[projectId]/data-sources/[dataSourceId]/edit"
                  as={`/${projectId}/data-sources/${dataSourceId}/edit`}
                  passHref
                >
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={onClick}
                  >
                    Edit
                  </Button>
                </Link>
              </li>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={() => {
                    onClick()
                    setDeleteModalOpen(true)
                  }}
                >
                  Delete
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>

      {isDeleteModalOpen && (
        <Modal
          title="Delete Data Source"
          message={`Are you sure you want to delete the "${name}" data source? This cannot be undone.`}
          action={isDeleting ? 'Deleting...' : 'Delete Permanently'}
          onCancel={() => {
            setDeleteModalOpen(false)
          }}
          onConfirm={async () => {
            setIsDeleting(true)

            await fetcher(
              `/api/v1/projects/${projectId}/data_sources/${dataSourceId}/`,
              { method: 'DELETE' },
            )

            await revalidate()

            Router.push(
              '/[projectId]/data-sources?action=delete-datasource-success',
              `/${projectId}/data-sources`,
            )
          }}
        />
      )}
    </>
  )
}

DataSourcesMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  dataSourceId: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default DataSourcesMenu
