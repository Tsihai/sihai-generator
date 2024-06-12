import {
  getGeneratorVoByIdUsingGet,
  useGeneratorUsingPost,
} from '@/services/backend/generatorController';
import { Link, useModel, useParams } from '@@/exports';
import {CreditCardOutlined, DownloadOutlined} from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import {
  Button,
  Card,
  Col,
  Collapse,
  Form,
  Image,
  Input,
  message,
  Radio,
  Row,
  Space,
  Tag,
  Typography,
} from 'antd';
import { saveAs } from 'file-saver';
import React, { useEffect, useState } from 'react';

/**
 * 生成器使用
 * @constructor
 */
const GeneratorUsePage: React.FC = () => {
  const { id } = useParams();
  const [form] = Form.useForm();

  const [loading, setLoading] = useState<boolean>(false);
  const [downloading, setDownloading] = useState<boolean>(false);
  const [data, setData] = useState<API.GeneratorVO>({});
  const { initialState } = useModel('@@initialState');
  const { currentUser } = initialState ?? {};

  const models = data?.modelConfig?.models ?? [];

  /**
   * 设置初始值
   * @param models
   */
  const modelInitialValues = (models: API.ModelInfo[]) => {
    let res = {};
    models?.forEach((mode) => {
      if (mode.groupKey) {
        res = {
          ...res,
          // @ts-ignore
          [mode.groupKey]: modelInitialValues(mode.models),
        };
      } else {
        res = {
          ...res,
          // @ts-ignore
          [mode.fieldName]: mode.defaultValue,
        };
      }
    });
    return res;
  };
  let defaultValue = modelInitialValues(models);
  const [formValues, setFormValues] = useState<any>({ ...defaultValue });
  const isEmptyObject = (obj: any) => Object.entries(obj).length === 0;

  /**
   * 加载数据
   */
  const loadData = async () => {
    if (!id) {
      return;
    }
    setLoading(true);
    try {
      const res = await getGeneratorVoByIdUsingGet({
        id,
      });
      setData(res.data || {});
    } catch (error: any) {
      message.error('获取数据失败，' + error.message);
    }
    setLoading(false);
  };

  useEffect(() => {
    loadData();
  }, [id]);

  /**
   * 标签列表视图
   * @param tags
   */
  const tagListView = (tags?: string[]) => {
    if (!tags) {
      return <></>;
    }

    return (
      <div style={{ marginBottom: 8 }}>
        {tags.map((tag: string) => {
          return <Tag key={tag}>{tag}</Tag>;
        })}
      </div>
    );
  };

  /**
   * 下载按钮
   */
  const downloadButton = data.distPath && currentUser && (
    <Button
      type="primary"
      icon={<DownloadOutlined />}
      loading={downloading}
      onClick={async () => {
        setDownloading(true);
        const values = form.getFieldsValue();

        // eslint-disable-next-line react-hooks/rules-of-hooks
        const blob = await useGeneratorUsingPost(
          {
            id: data.id,
            dataModel: values,
          },
          {
            responseType: 'blob',
          },
        );
        // 使用 file-saver 来保存文件
        const fullPath = data.distPath || '';
        saveAs(blob, fullPath.substring(fullPath.lastIndexOf('/') + 1));
        setDownloading(false);
      }}
    >
      生成代码
    </Button>
  );

  /**
   * 详情按钮
   */
  const detailButton = (
    <Link to={`/generator/detail/${data.id}`}>
      <Button icon={<CreditCardOutlined />}>查看详情</Button>
    </Link>
  );

  return (
    <PageContainer title={<></>} loading={loading}>
      <Card>
        <Row justify="space-between" gutter={[32, 32]}>
          <Col flex="auto">
            <Space size="large" align="center">
              <Typography.Title level={4}>{data.name}</Typography.Title>
              {tagListView(data.tags)}
            </Space>
            <Typography.Paragraph>{data.description}</Typography.Paragraph>

            <div style={{ marginBottom: 24 }} />
              <Form
                form={form}
                initialValues={defaultValue}
                onValuesChange={(changedValues, allValues) => setFormValues(allValues)}
              >
              {models.map((model, index) => {
                // 是分组
                if (model.groupKey) {
                  if (!model.models) {
                    return <></>;
                  }

                  return (
                    <Collapse
                      key={index}
                      style={{
                        marginBottom: 24,
                      }}
                      items={[
                        {
                          key: index,
                          label: model.groupName + '（分组）',
                          children: model.models.map((subModel, index) => {
                            return (
                              <Form.Item
                                key={index}
                                label={subModel.fieldName}
                                // @ts-ignore
                                name={[model.groupKey, subModel.fieldName]}
                              >
                                <Input placeholder={subModel.description} />
                              </Form.Item>
                            );
                          }),
                        },
                      ]}
                      bordered={false}
                      defaultActiveKey={[index]}
                    />
                  );
                }

                return model.type === 'Boolean' ? (
                  <Form.Item
                    label={model.description + `(${model.fieldName})`}
                    name={model.fieldName}>
                    <Radio.Group>
                      <Radio value={true}>是</Radio>
                      <Radio value={false}>否</Radio>
                    </Radio.Group>
                  </Form.Item>
                ) : (
                  <Form.Item
                    label={model.description + `(${model.fieldName})`}
                    name={model.fieldName}
                  >
                    <Input placeholder={model.description} />
                  </Form.Item>
                )
              })}
            </Form>
            <Space size="middle">
              {downloadButton}
              {detailButton}
            </Space>
          </Col>
          <Col flex="320px">
            <Image src={data.picture} />
          </Col>
        </Row>
      </Card>
    </PageContainer>
  );
};

export default GeneratorUsePage;
